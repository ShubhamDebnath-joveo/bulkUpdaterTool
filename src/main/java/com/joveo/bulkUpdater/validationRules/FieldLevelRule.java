package com.joveo.bulkUpdater.validationRules;

import com.joveo.bulkUpdater.model.ValidationResult;
import org.apache.commons.csv.CSVRecord;

import java.util.ArrayList;
import java.util.List;

public abstract class FieldLevelRule {

    public ValidationResult validate(CSVRecord record) throws RuntimeException {return new ValidationResult(true, new ArrayList<>(), record);};

    public String errMsg(){ return "";};
}
