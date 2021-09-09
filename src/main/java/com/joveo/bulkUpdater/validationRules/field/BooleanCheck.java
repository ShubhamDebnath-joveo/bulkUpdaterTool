package com.joveo.bulkUpdater.validationRules.field;

import com.joveo.bulkUpdater.model.ValidationResult;
import com.joveo.bulkUpdater.validationRules.FieldLevelRule;
import lombok.AllArgsConstructor;
import org.apache.commons.csv.CSVRecord;

@AllArgsConstructor
public class BooleanCheck extends FieldLevelRule {

    private String columnName;

    private FieldLevelRule rule;

    @Override
    public ValidationResult validate(CSVRecord record) {
        ValidationResult result = this.rule.validate(record);

        if(record.get(columnName).isBlank()){
            return result;
        }

        String val = record.get(columnName);
        if(!val.equalsIgnoreCase("true") && !val.equalsIgnoreCase("true")){
            result.updateResult(false, String.format(this.errMsg(), val, columnName, record.getRecordNumber()));
        }

        return result;
    }

    @Override
    public String errMsg() {
        return "Boolean expected: but got :%s  for %s at row number: %d";
    }
}
