package com.joveo.bulkUpdater.validationRules.field;

import com.joveo.bulkUpdater.model.JoveoException;
import com.joveo.bulkUpdater.model.ValidationResult;
import com.joveo.bulkUpdater.validationRules.FieldLevelRule;
import org.apache.commons.csv.CSVRecord;

import java.util.HashSet;
import java.util.Set;

public class DuplicateCheck extends FieldLevelRule {

    private String columnName;
    private FieldLevelRule rule;
    private Set<String> values;

    public DuplicateCheck(String columnName, FieldLevelRule rule){
        this.columnName = columnName;
        this.rule = rule;
        this.values = new HashSet<>();
    }

    @Override
    public ValidationResult validate(CSVRecord record) {
        ValidationResult result = this.rule.validate(record);

        if(this.values.contains(record.get(columnName))){
            result.updateResult(false, String.format(this.errMsg(), columnName, record.getRecordNumber()));
        }

        this.values.add(record.get(columnName));
        return result;
    }

    @Override
    public String errMsg() {
        return "ignoring column: %s for row num: %d , already seen above";
    }
}