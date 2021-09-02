package com.joveo.bulkUpdater.validationRules;

import com.joveo.bulkUpdater.CliUtils;
import com.joveo.bulkUpdater.validationRules.field.BlankCheck;
import com.joveo.bulkUpdater.validationRules.field.DuplicateCheck;
import com.joveo.eqrtestsdk.core.entities.Driver;
import com.joveo.eqrtestsdk.models.JobGroupDto;
import org.apache.commons.csv.CSVRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EditFlow extends Flow {

    private static List<String> coreFields = List.of("jobGroupId");

    public EditFlow(Set<String> headers, Driver driver) {
        super(headers, driver);
    }

    @Override
    public FieldLevelRule buildRules() {
        FieldLevelRule rule = new FieldLevelRule() {};

        for (String colName : headers) {
            if (coreFields.contains(colName)) {
                rule = new BlankCheck(colName, rule);
                rule = new DuplicateCheck(colName, rule);
            }
        }
        return rule;
    }

    @Override
    public void processRecord(CSVRecord record) {

    }

    private JobGroupDto recordToJGUpdate(CSVRecord record) {

        JobGroupDto d = new JobGroupDto();
        d.setClientId(CliUtils.getOption(CliUtils.CLIENT_ID));
        d.setJobGroupId(record.get("jobGroupId"));

        List<JobGroupDto.JobGroupParams.Placements> arr = new ArrayList<>();
        for(int i = lowestNumPlacement; i <= highestNumPlacement; i++) {
            String keyPrefix = "placements_" + i + "_";
            if(record.get(keyPrefix + "bid").equals("#N/A") || record.get(keyPrefix + "bid").equals("NA") || record.get(keyPrefix + "bid").isBlank() ){
                continue;
            }
            JobGroupDto.JobGroupParams.Placements p = new JobGroupDto.JobGroupParams.Placements(record.get(keyPrefix + "value"));
            p.setBid(Double.parseDouble(record.get(keyPrefix + "bid")));
            arr.add(p);
        }
        d.setPlacements(arr);
        return d;
    }
}
