package com.joveo.bulkUpdater.validationRules.field;

import com.joveo.bulkUpdater.model.ValidationResult;
import com.joveo.bulkUpdater.validationRules.FieldLevelRule;
import lombok.AllArgsConstructor;
import org.apache.commons.csv.CSVRecord;

@AllArgsConstructor
public class PlacementNameCheck extends FieldLevelRule {

    private String columnName;

    private FieldLevelRule rule;

    @Override
    public ValidationResult validate(CSVRecord record) {
        ValidationResult result = this.rule.validate(record);

        if(record.get(columnName).isBlank()){
            return result;
        }else{
            if(record.get(columnName.replace("bid", "value")).isBlank()){
                result.updateResult(false, String.format(this.errMsg(), columnName, record.getRecordNumber()));
            }
        }

        return result;
    }

    @Override
    public String errMsg() {
        return "corresponding placement name has to be defined since %s is defined at row number: %d";
    }
}