package com.joveo.bulkUpdater.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.csv.CSVRecord;

import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class ValidationResult {
    private boolean valid;
    private List<String> messages;
    private CSVRecord record;

    public ValidationResult updateResult(boolean valid, String message){
        this.valid &= valid;
        this.messages.add(message);
        return this;
    }
}
