package com.joveo.bulkUpdater.validationRules.field;

import com.joveo.bulkUpdater.model.ValidationResult;
import com.joveo.bulkUpdater.validationRules.FieldLevelRule;
import com.joveo.eqrtestsdk.models.RuleOperator;
import lombok.AllArgsConstructor;
import org.apache.commons.csv.CSVRecord;

@AllArgsConstructor
public class OperatorCheck extends FieldLevelRule {

    private String columnName;

    private FieldLevelRule rule;

    @Override
    public ValidationResult validate(CSVRecord record) {
        ValidationResult result = this.rule.validate(record);

        if(record.get(columnName).isBlank()){
            return result;
        }
        try {
            RuleOperator.valueOf(record.get(columnName));
        }catch (IllegalArgumentException ile){
            if(record.get(columnName).equalsIgnoreCase("ALL") || record.get(columnName).equalsIgnoreCase("OR") ||
                    record.get(columnName).equalsIgnoreCase("AND") || record.get(columnName).equalsIgnoreCase("ANY")){
                return result;
            }
            result.updateResult(false, String.format(this.errMsg(), record.get(columnName), columnName, record.getRecordNumber()));
        }

        return result;
    }

    @Override
    public String errMsg() {
        return "Valid operator expected but got: %s  for %s at row number: %d";
    }
}
