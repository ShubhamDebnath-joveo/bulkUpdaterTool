package com.joveo.bulkUpdater.validationRules;

import com.joveo.bulkUpdater.CliUtils;
import com.joveo.bulkUpdater.model.JoveoException;
import com.joveo.bulkUpdater.validationRules.field.*;
import com.joveo.eqrtestsdk.core.entities.Driver;
import com.joveo.eqrtestsdk.core.entities.JobGroup;
import com.joveo.eqrtestsdk.models.CapDto;
import com.joveo.eqrtestsdk.models.Freq;
import com.joveo.eqrtestsdk.models.JobGroupDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// sagar - ask for default on caps values incase of partial fields
// vishal - ask for activaeSTatus field on jobGroup
@Slf4j
public class EditFlow extends Flow {

    private static List<String> coreFields = List.of("jobGroupId");
    private static List<String> doubleFields = List.of("cpcBid", "cpaBid", "caps_budget_value", "caps_clicks_value",
            "caps_applies_value", "caps_budget_thresholdP", "caps_clicks_thresholdP",
            "caps_applies_thresholdP");
    private static List<String> placementFields = List.of("value", "bid", "caps_budget_cap",
            "caps_budget_value", "caps_budget_pacing", "caps_budget_thresholdP", "caps_budget_locked", "is_active");

    public EditFlow(Set<String> headers, Driver driver) {
        super(headers, driver);
    }

    @Override
    public FieldLevelRule buildRules() {
        FieldLevelRule rule = new FieldLevelRule() {
        };

        if (!headers.contains("jobGroupId")) {
            throw new JoveoException("jobGroupId not found in sheet");
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
            }
        }

        for (String colName : headers) {

            if(colName.equals("jobGroupId")){
                rule = new DefaultJGCheck(colName, rule);
            }

            if (coreFields.contains(colName)) {
                rule = new BlankCheck(colName, rule);
                rule = new DuplicateCheck(colName, rule);
            }

            if (doubleFields.contains(colName)) {
                rule = new DoubleCheck(colName, rule);
            }
        }
        return rule;
    }

    @Override
    public void processRecord(CSVRecord record) {
        JobGroupDto jgDto = recordToJGUpdate(record);
        try {
            JobGroup jobGroup = driver.getExistingJobGroup(CliUtils.getOption(CliUtils.CLIENT_ID), record.get("jobGroupId"));

            if (record.isMapped("jobGroup_activeStatus") && !isEmpty(record.get("jobGroup_activeStatus"))) {
                String status = record.get("jobGroup_activeStatus");
                if (status.equals("A")) {
                    jobGroup.enableJobGroup();
                } else if (status.equals("P")) {
                    jobGroup.pauseJobGroup();
                }
            }

            jobGroup.edit(jgDto);
            log.info("Updated " + record.get("jobGroupId") + " for name " + record.get("jobGroupName"));
        } catch (Exception e) {
            String msg = "Record: " + record.getRecordNumber() + " Failed for " + record.get("jobGroupId") + " for name " + record.get("jobGroupName") + " because " + e.getMessage();
            log.info(msg);
            failedRows.add(record.getRecordNumber() + "," + record.get("jobGroupId") + "," + e.getMessage());
        }

    }

    private JobGroupDto recordToJGUpdate(CSVRecord record) {

        boolean pacing = false;
        Freq freq = null;
        double threshold = -1;
        double value = -1;
        boolean locked = false;

        JobGroupDto jgDto = new JobGroupDto();
        jgDto.setClientId(CliUtils.getOption(CliUtils.CLIENT_ID));
        jgDto.setJobGroupId(record.get("jobGroupId"));

        if (record.isMapped("jobGroupName") && !isEmpty(record.get("jobGroupName")))
            jgDto.setName(record.get("jobGroupName"));

/*
         ignoring startDate, it is not editable
*/
//        if (record.isMapped("startDate") && !isEmpty(record.get("startDate")))
//            d.setStartDate(LocalDate.parse(record.get("startDate")));

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

        List<JobGroupDto.JobGroupParams.Placements> placements = new ArrayList<>();
        for (int i = lowestNumPlacement; i <= highestNumPlacement; i++) {
            String keyPrefix = "placements_" + i + "_";
            if (isEmpty(record.get(keyPrefix + "bid"))) {
                continue;
            }
            JobGroupDto.JobGroupParams.Placements placement = new JobGroupDto.JobGroupParams.Placements(record.get(keyPrefix + "value"));
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
        jgDto.setPlacements(placements);
        return jgDto;
    }
}
