package com.joveo.bulkUpdater.validationRules;

import com.joveo.eqrtestsdk.core.entities.Driver;
import com.joveo.eqrtestsdk.models.JobGroupDto;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.FileWriter;
import java.io.IOException;
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
    protected String fileName;
    protected List<String> failedRows;

    public Flow(Set<String> headers, Driver driver){
        this.headers = headers;
        this.driver = driver;
        setNumPlacement();
        fileName = "rows_sdk_failure.csv";
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
        String outputFile = fileName;
        CSVPrinter csvFilePrinter = null;
        CSVFormat csvFileFormat = CSVFormat.EXCEL.builder().build();
        FileWriter fileWriter = new FileWriter(outputFile);
        csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);

        csvFilePrinter.printRecords(failedRows);


        fileWriter.flush();
        fileWriter.close();
        csvFilePrinter.close();
    }

}
