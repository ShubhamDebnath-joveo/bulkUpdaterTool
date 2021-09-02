package com.joveo.bulkUpdater.validationRules;

import com.joveo.bulkUpdater.CliUtils;
import com.joveo.eqrtestsdk.core.entities.Driver;
import com.joveo.eqrtestsdk.models.Freq;
import com.joveo.eqrtestsdk.models.JobFilter;
import com.joveo.eqrtestsdk.models.JobFilterFields;
import com.joveo.eqrtestsdk.models.JobGroupDto;
import org.apache.commons.csv.CSVRecord;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Set;

public class CreateFlow extends Flow {

    public CreateFlow(Set<String> headers, Driver driver) {
        super(headers, driver);
    }

    @Override
    public FieldLevelRule buildRules() {
        return null;
    }

    @Override
    public void processRecord(CSVRecord record) {

    }

    public static JobGroupDto recordToJG(CSVRecord record, int highestPlacementNumber) {
        JobGroupDto d = new JobGroupDto();
        d.setClientId(CliUtils.getOption(CliUtils.CLIENT_ID));
        d.setName(record.get("name"));
//    d.setCampaignId("c2a09a51-4bfd-4aae-be29-11b7e77a7f8f$campaign$default");
        d.setCpcBid(Double.parseDouble(record.get("cpcBid")));


        d.setCampaignId(record.get("campaignId"));


        d.setStartDate(LocalDate.parse("8/18/2021", DateTimeFormatter.ofPattern("M/dd/yyyy")));
//    d.setStartDate(LocalDate.parse(record.get("startDate"), DateTimeFormatter.ofPattern("M/dd/yyyy")));
        d.setEndDate(LocalDate.parse(record.get("endDate"), DateTimeFormatter.ofPattern("M/dd/yyyy")));
//    d.setBudgetCap(false, Freq.Weekly, Double.parseDouble(record.get("caps_budget_thresholdP")), Double.parseDouble(record.get("caps_budget_value")));
        d.setBudgetCap(false, Freq.Weekly, Double.parseDouble(record.get("caps_budget_thresholdP")), Double.parseDouble(record.get("caps_budget_value")));
        d.setJobFilter(JobFilter.and(
                JobFilter.eq(JobFilterFields.country, record.get("filters_operator_rules_data_1_1")),
                JobFilter.eq(JobFilterFields.city, record.get("filters_operator_rules_data_1_2"))
        ));
        ArrayList<JobGroupDto.JobGroupParams.Placements> arr = new ArrayList<>();
        for(int i = 1; i <= highestPlacementNumber; i++) {
            String keyPrefix = "placements_" + i + "_";
            if(record.get(keyPrefix + "bid").equals("#N/A") || record.get(keyPrefix + "bid").equals("NA") || record.get(keyPrefix + "bid").equals("") ){
                continue;
            }
            JobGroupDto.JobGroupParams.Placements p = new JobGroupDto.JobGroupParams.Placements(record.get(keyPrefix + "value"));
            if(record.get(keyPrefix + "bid") != "#N/A" && record.get(keyPrefix + "bid") != "" ) {
                p.setBid(Double.parseDouble(record.get(keyPrefix + "bid")));
            }
            arr.add(p);
        }
        d.setPlacements(arr);
        return d;
    }
}
