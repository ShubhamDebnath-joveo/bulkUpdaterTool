package com.joveo.bulkUpdater.validationRules.field;

import com.joveo.bulkUpdater.model.ValidationResult;
import com.joveo.bulkUpdater.util.Util;
import com.joveo.bulkUpdater.validationRules.FieldLevelRule;
import com.joveo.eqrtestsdk.models.Freq;
import lombok.AllArgsConstructor;
import org.apache.commons.csv.CSVRecord;

import java.util.Arrays;

@AllArgsConstructor
public class FreqCheck extends FieldLevelRule {

    private String columnName;

    private FieldLevelRule rule;

    @Override
    public ValidationResult validate(CSVRecord record) {
        ValidationResult result = this.rule.validate(record);

        String val = record.get(columnName);
        if(val.isBlank()){
            return result;
        }
        Freq freq = Util.searchEnum(Freq.class, val);
        if(freq == null){
            result.updateResult(false, String.format(this.errMsg(), val, columnName, record.getRecordNumber(), Arrays.toString(Freq.values())));
        }

        return result;
    }

    @Override
    public String errMsg() {
        return "%s is not a valid cap value for column: %s for row num: %d , allowed values are: %s";
    }
}