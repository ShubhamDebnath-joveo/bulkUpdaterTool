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


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


@Slf4j
public class EditFlow extends Flow {

    private static List<String> mandatoryFields = List.of("jobGroupId");
    private static List<String> doubleFields = List.of("cpcBid", "cpaBid", "caps_budget_value", "caps_clicks_value",
            "caps_applies_value", "caps_budget_thresholdP", "caps_clicks_thresholdP",
            "caps_applies_thresholdP");
    private static List<String> booleanFields = List.of("caps_budget_pacing", "caps_budget_locked", "caps_clicks_pacing",
            "caps_clicks_locked","caps_applies_pacing", "caps_applies_locked");
    private static List<String> placementFields = List.of("value", "bid", "caps_budget_cap",
            "caps_budget_value", "caps_budget_pacing", "caps_budget_thresholdP", "caps_budget_locked", "is_active");

    public EditFlow(Set<String> headers, Driver driver) {
        super(headers, driver);
    }

/*
 check first non-empty row for header
 if edit mode , then only jobgroupId mandatory
 name may be duplicate, but corresponding jobGroupId cant be
 separate validate and run methods (as an interface) for create and edit mode
 under dry-run mode, only run validate
 1 class for reading and building object
 1 interface for edit or create mode, it will have validate and run function, in edit mode only validate

 2 document level rules
 all required columns present or not
 each placement is present in group of 7 fields
 */

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

                if(field.equals("is_active") || field.equals("caps_budget_locked") || field.equals("caps_budget_pacing")){
                    rule = new BooleanCheck(keyPrefix, rule);
                }
            }
        }

        for (String colName : headers) {

            if(colName.equals("jobGroupId")){
                rule = new DefaultJGCheck(colName, rule);
            }

            if (mandatoryFields.contains(colName)) {
                rule = new BlankCheck(colName, rule);
                rule = new DuplicateCheck(colName, rule);
            }

            if (doubleFields.contains(colName)) {
                rule = new DoubleCheck(colName, rule);
            }

            if (booleanFields.contains(colName)) {
                rule = new BooleanCheck(colName, rule);
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
        boolean active = false;
        String placementName = "";

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
        jgDto.setPlacements(placements);
        return jgDto;
    }
}
