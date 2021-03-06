package com.joveo.bulkUpdater.validationRules.field;

import com.joveo.bulkUpdater.model.ValidationResult;
import com.joveo.bulkUpdater.util.Util;
import com.joveo.bulkUpdater.validationRules.FieldLevelRule;
import com.joveo.eqrtestsdk.models.RuleOperator;
import lombok.AllArgsConstructor;
import org.apache.commons.csv.CSVRecord;

import java.util.Arrays;

@AllArgsConstructor
public class OperatorCheck extends FieldLevelRule {

    private String columnName;

    private FieldLevelRule rule;

    @Override
    public ValidationResult validate(CSVRecord record) {
        ValidationResult result = this.rule.validate(record);

        String val = record.get(columnName);

        if(val.isBlank()){
            return result;
        }

        RuleOperator operator = Util.searchEnum(RuleOperator.class, val);
        if(operator == null){
            if(val.equalsIgnoreCase("ALL") || val.equalsIgnoreCase("AND") ||
            val.equalsIgnoreCase("OR") || val.equalsIgnoreCase("ANY")){
                return result;
            }

            result.updateResult(false, String.format(this.errMsg(), record.get(columnName),
                    columnName, record.getRecordNumber(), Arrays.toString(RuleOperator.values())));
        }

        return result;
    }

    @Override
    public String errMsg() {
        return "Valid operator expected but got: %s  for %s at row number: %d, valid operators are ALL, AND, OR, ANY, %s";
    }
}
