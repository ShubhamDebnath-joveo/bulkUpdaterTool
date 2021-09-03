package com.joveo.bulkUpdater.validationRules;

import com.joveo.bulkUpdater.util.Util;
import com.joveo.eqrtestsdk.core.entities.Driver;
import com.joveo.eqrtestsdk.models.JobGroupDto;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Flow {

    protected int lowestNumPlacement = -1;
    protected int highestNumPlacement = -1;
    protected Set<String> headers = null;
    protected Driver driver;
    protected static final String CSV_FILE_NAME = "rows_sdk_failure.csv";
    protected List<String> failedRows;

    public Flow(Set<String> headers, Driver driver){
        this.headers = headers;
        this.driver = driver;
        setNumPlacement();
        failedRows = new ArrayList<>();
    }

    public abstract FieldLevelRule buildRules();

    public abstract void processRecord(CSVRecord record);

    protected void setNumPlacement(){

        Pattern pattern = Pattern.compile("^placements_([\\d]+)_.*", Pattern.CASE_INSENSITIVE);
        TreeSet<Integer> placementNumbers = new TreeSet<>();

        for(String s : headers) {
            Matcher m = pattern.matcher(s);

            if(m.find()) {
                placementNumbers.add(Integer.parseInt(m.group(1)));
            }
        }

        lowestNumPlacement = placementNumbers.first();
        highestNumPlacement = placementNumbers.last();
    }

    protected boolean isEmpty(String str){
        return (str.equals("#N/A") || str.equals("NA") || str.isBlank());
    }

    public void writeFailedRows() throws IOException {
        File csvOutputFile = new File(CSV_FILE_NAME);
        try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
            pw.println("RowNumber,jobGroupId,Reason");
            failedRows.stream()
                    .map(d -> Util.escapeSpecialCharacters(d))
                    .forEach(pw::println);
        }
    }

}
