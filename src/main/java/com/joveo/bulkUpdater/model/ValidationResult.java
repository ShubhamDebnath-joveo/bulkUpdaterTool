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

    public void updateResult(boolean valid, String message){
        this.valid &= valid;
        this.messages.add(message);
    }

    @Override
    public String toString(){
        return this.record.getRecordNumber() + "," + record.get("jobGroupId") + ","  + String.join(",", this.messages);
    }
}
