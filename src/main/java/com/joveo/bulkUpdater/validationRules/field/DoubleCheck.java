package com.joveo.bulkUpdater.validationRules.field;

import com.joveo.bulkUpdater.model.ValidationResult;
import com.joveo.bulkUpdater.validationRules.FieldLevelRule;
import lombok.AllArgsConstructor;
import org.apache.commons.csv.CSVRecord;

@AllArgsConstructor
public class DoubleCheck extends FieldLevelRule {

    private String columnName;

    private FieldLevelRule rule;

    @Override
    public ValidationResult validate(CSVRecord record) {
        ValidationResult result = this.rule.validate(record);

        if(record.get(columnName).isBlank()){
            return result;
        }
        try {
            Double.parseDouble(record.get(columnName));
        }catch (NumberFormatException nfe){
            result.updateResult(false, String.format(this.errMsg(), nfe.getMessage(), columnName, record.getRecordNumber()));
        }

        return result;
    }

    @Override
    public String errMsg() {
        return "%s  for %s at row number: %d";
    }
}
