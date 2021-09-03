package com.joveo.bulkUpdater.validationRules.field;

import com.joveo.bulkUpdater.model.ValidationResult;
import com.joveo.bulkUpdater.validationRules.FieldLevelRule;
import lombok.AllArgsConstructor;
import org.apache.commons.csv.CSVRecord;

@AllArgsConstructor
public class DefaultJGCheck extends FieldLevelRule {

    private String columnName;

    private FieldLevelRule rule;

    @Override
    public ValidationResult validate(CSVRecord record) {
        ValidationResult result = this.rule.validate(record);

        if(record.get(columnName).contains("$jobgroup$default")){
            result.updateResult(false, String.format(this.errMsg(), record.getRecordNumber()));
        }

        return result;
    }

    @Override
    public String errMsg() {
        return "Default jobGroup will be ignored for row num: %d";
    }
}
