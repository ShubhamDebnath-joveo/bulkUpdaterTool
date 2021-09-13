package com.joveo.bulkUpdater.validationRules;

import com.joveo.bulkUpdater.CliUtils;
import com.joveo.bulkUpdater.model.JoveoException;
import com.joveo.bulkUpdater.validationRules.field.*;
import com.joveo.eqrtestsdk.core.entities.Driver;
import com.joveo.eqrtestsdk.core.entities.JobGroup;
import com.joveo.eqrtestsdk.models.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
public class CreateFlow extends Flow {

    public CreateFlow(Set<String> headers, Driver driver) {
        super(headers, driver);
    }

    private static List<String> mandatoryFields = List.of("jobGroupName", "campaignId", "startDate", "endDate", "caps_budget_cap",
            "caps_budget_value", "caps_budget_pacing", "caps_budget_thresholdP", "caps_budget_locked");
    private static List<String> doubleFields = List.of("cpcBid", "cpaBid", "caps_budget_value", "caps_clicks_value",
            "caps_applies_value", "caps_budget_thresholdP", "caps_clicks_thresholdP",
            "caps_applies_thresholdP");
    private static List<String> booleanFields = List.of("caps_budget_pacing", "caps_budget_locked", "caps_clicks_pacing",
            "caps_clicks_locked","caps_applies_pacing", "caps_applies_locked");
    private static List<String> placementFields = List.of("value", "bid", "caps_budget_cap",
            "caps_budget_value", "caps_budget_pacing", "caps_budget_thresholdP", "caps_budget_locked", "is_active");

    @Override
    public FieldLevelRule buildRules() {

        FieldLevelRule rule = new FieldLevelRule() {
        };

        for(String coreField: mandatoryFields){
            if (!headers.contains(coreField) ) {
                throw new JoveoException(coreField + " not found in sheet");
            }
        }

        for (int i = lowestNumPlacement; i <= highestNumPlacement; i++) {
            for (String field : placementFields) {
                String keyPrefix = "placements_" + i + "_" + field;
                if (!headers.contains(keyPrefix)) {
                    throw new JoveoException(keyPrefix + " not found in sheet");
                }

                if(field.equals("bid")){
                    rule = new PlacementNameCheck(keyPrefix, rule);
                }

                if(keyPrefix.contains("bid") || keyPrefix.contains("budget_value") ||keyPrefix.contains("thresholdP")){
                    rule = new DoubleCheck(keyPrefix, rule);
                }

                if(field.equals("is_active") || field.equals("caps_budget_locked") || field.equals("caps_budget_pacing")){
                    rule = new BooleanCheck(keyPrefix, rule);
                }
            }
        }

        for (String colName : headers) {

            if (mandatoryFields.contains(colName)) {
                rule = new BlankCheck(colName, rule);
            }

            if(colName.equalsIgnoreCase("jobGroupName")){
                rule = new DuplicateCheck(colName, rule);
            }

            if (doubleFields.contains(colName)) {
                rule = new DoubleCheck(colName, rule);
            }

            if (booleanFields.contains(colName)) {
                rule = new BooleanCheck(colName, rule);
            }

            if(colName.contains("rules_operator")){
                rule = new OperatorCheck(colName, rule);
            }
        }
        return rule;
    }

    @Override
    public void processRecord(CSVRecord record) {
        try {
            JobGroupDto jgDto = recordToJG(record);
            JobGroup jobGroup = driver.createJobGroup(jgDto, true);

            if (record.isMapped("jobGroup_activeStatus") && !isEmpty(record.get("jobGroup_activeStatus"))) {
                String status = record.get("jobGroup_activeStatus");
                if (status.equals("A")) {
                    jobGroup.enableJobGroup();
                } else if (status.equals("P")) {
                    jobGroup.pauseJobGroup();
                }
            }

            log.info("Created " + jobGroup.getId() + " for name " + record.get("jobGroupName"));
        } catch (Exception e) {
            String msg = "Record: " + record.getRecordNumber() + " for name " + record.get("jobGroupName") + " because " + e.getMessage();
            log.info(msg);
            failedRows.add(record.getRecordNumber() + "," + record.get("jobGroupName") + "," + e.getMessage());
        }
    }

    public JobGroupDto recordToJG(CSVRecord record) {

        boolean pacing = false;
        Freq freq = null;
        double threshold = -1;
        double value = -1;
        boolean locked = false;
        boolean active = false;
        String placementName = "";
        String filterOperator = "ALL";
        GroupingJobFilter filter = null;
        List<Filter> rules = null;
        String field = null;
        String operator = null;
        String data = null;

        JobGroupDto jgDto = new JobGroupDto();
        jgDto.setClientId(CliUtils.getOption(CliUtils.CLIENT_ID));
        jgDto.setName(record.get("jobGroupName"));
        jgDto.setCampaignId(record.get("campaignId"));

        if (record.isMapped("startDate") && !isEmpty(record.get("startDate")))
            jgDto.setStartDate(LocalDate.parse(record.get("startDate")));

            if (record.isMapped("endDate") && !isEmpty(record.get("endDate")))
                jgDto.setEndDate(LocalDate.parse(record.get("endDate")));

        if (record.isMapped("cpcBid") && !isEmpty(record.get("cpcBid")))
            jgDto.setCpcBid(Double.parseDouble(record.get("cpcBid")));

        if (record.isMapped("cpaBid") && !isEmpty(record.get("cpaBid")))
            jgDto.setCpaBid(Double.parseDouble(record.get("cpaBid")));


        for(String capType: List.of("budget", "clicks", "applies")){
            String capPrefix = "caps_" + capType + "_";

            if(record.isMapped(capPrefix + "value") && !isEmpty(record.get(capPrefix + "value"))){
                pacing = record.isMapped(capPrefix + "pacing") && !isEmpty(record.get(capPrefix + "pacing")) && Boolean.parseBoolean(record.get(capPrefix + "pacing"));
                freq = record.isMapped(capPrefix + "cap") && !isEmpty(record.get(capPrefix + "cap")) ? Freq.valueOf(record.get(capPrefix + "cap")) : Freq.Monthly;
                threshold = record.isMapped(capPrefix + "thresholdP") && !isEmpty(record.get(capPrefix + "thresholdP")) ? Double.parseDouble(record.get(capPrefix + "thresholdP")) : 80;
                value = Double.parseDouble(record.get(capPrefix + "value"));

                if(capType.equals("budget"))
                    jgDto.setBudgetCap(pacing, freq, threshold, value);
                else if(capType.equals("clicks"))
                    jgDto.setClickCap(pacing, freq, threshold, (int)value);
                else
                    jgDto.setApplyCap(pacing, freq, threshold, (int)value);
            }
        }

        filterOperator = record.get("filters_operator");

        if(filterOperator.equalsIgnoreCase("ALL") || filterOperator.equalsIgnoreCase("AND")){
            filter = new GroupingJobFilter(GroupOperator.AND, null);
        }
        else if(filterOperator.equalsIgnoreCase("ANY") || filterOperator.equalsIgnoreCase("OR")){
            filter = new GroupingJobFilter(GroupOperator.OR, null);
        }

        rules = new ArrayList<>();
        for (int i = lowestNumFilter; i <= highestNumFilter; i++) {
            field = null;
            operator = null;
            data = null;
            String filterPrefix = "filters_operator_rules_";

            if (record.isMapped(filterPrefix + "field_1_" + i) && !isEmpty(record.get(filterPrefix + "field_1_" + i))){
                field = record.get(filterPrefix + "field_1_" + i);
            }

            if (record.isMapped(filterPrefix + "operator_1_" + i) && !isEmpty(record.get(filterPrefix + "operator_1_" + i))){
                operator = record.get(filterPrefix + "operator_1_" + i);
            }

            if (record.isMapped(filterPrefix + "data_1_" + i) && !isEmpty(record.get(filterPrefix + "data_1_" + i))){
                data = record.get(filterPrefix + "data_1_" + i);
            }

            if(field == null || operator == null || data == null){
                continue;
            }
            rules.add(new JobFilter(RuleOperator.valueOf(operator), field, data));
        }

        if(rules.size() == 0){
            throw new JoveoException("At least 1 rule needs to be specified in sheet to create a new jobGroup");
        }

        filter.setRules(rules);
        jgDto.setJobFilter(filter);

        List<JobGroupDto.JobGroupParams.Placements> placements = new ArrayList<>();
        for (int i = lowestNumPlacement; i <= highestNumPlacement; i++) {
            String keyPrefix = "placements_" + i + "_";

            placementName = record.get(keyPrefix + "value");
            if( record.isMapped(keyPrefix + "is_active") && !isEmpty(record.get(keyPrefix + "is_active"))) {
                active = Boolean.parseBoolean(record.get(keyPrefix + "is_active"));
                if(!active) {
                    jgDto.removePlacement(placementName);
                }
            }

            if (isEmpty(record.get(keyPrefix + "bid"))) {
                continue;
            }

            JobGroupDto.JobGroupParams.Placements placement = new JobGroupDto.JobGroupParams.Placements(placementName);
            placement.setBid(Double.parseDouble(record.get(keyPrefix + "bid")));

            if(record.isMapped(keyPrefix + "caps_budget_value") && !isEmpty(record.get(keyPrefix + "caps_budget_value"))) {
                pacing = record.isMapped(keyPrefix + "caps_budget_pacing") && !isEmpty(record.get(keyPrefix + "caps_budget_pacing")) && Boolean.parseBoolean(record.get(keyPrefix + "caps_budget_pacing"));
                freq = record.isMapped(keyPrefix + "caps_budget_cap") && !isEmpty(record.get(keyPrefix + "caps_budget_cap")) ? Freq.valueOf(record.get(keyPrefix + "caps_budget_cap")) : Freq.Monthly;
                threshold = record.isMapped(keyPrefix + "caps_budget_thresholdP") && !isEmpty(record.get(keyPrefix + "caps_budget_thresholdP")) ? Double.parseDouble(record.get(keyPrefix + "caps_budget_thresholdP")) : 80;
                value = Double.parseDouble(record.get(keyPrefix + "caps_budget_value"));
                locked = record.isMapped(keyPrefix + "caps_budget_locked") && !isEmpty(record.get(keyPrefix + "caps_budget_locked")) && Boolean.parseBoolean(record.get(keyPrefix + "caps_budget_locked"));

                placement.setBudget(new CapDto(pacing, freq, threshold, value, locked));

            }

            placements.add(placement);
        }

        if(placements.size() == 0){
            throw new JoveoException("At least 1 placement needs to be specified properly in sheet to create a new jobGroup");
        }

        jgDto.setPlacements(placements);
        return jgDto;
    }
}
