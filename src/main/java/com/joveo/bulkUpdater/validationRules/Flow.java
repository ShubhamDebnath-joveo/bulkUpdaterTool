package com.joveo.bulkUpdater.validationRules;

import com.joveo.bulkUpdater.util.Util;
import com.joveo.eqrtestsdk.core.entities.Driver;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Flow {

    protected int lowestNumPlacement = -1;
    protected int highestNumPlacement = -1;
    protected int lowestNumFilter = -1;
    protected int highestNumFilter = -1;
    protected Set<String> headers = null;
    protected Driver driver;
    protected static final String CSV_FILE_NAME = "mojo-errors.csv";
    protected List<String> failedRows;

    public Flow(Set<String> headers, Driver driver){
        this.headers = headers;
        this.driver = driver;
        setMinMaxNum();
        failedRows = new ArrayList<>();
    }

    public void setDriver(Driver driver){
        this.driver = driver;
    }

    public abstract FieldLevelRule buildRules();

    public abstract void processRecord(CSVRecord record);

    protected void setMinMaxNum(){

        Pattern patternPlacements = Pattern.compile("^placements_([\\d]+)_.*", Pattern.CASE_INSENSITIVE);
        TreeSet<Integer> placementNumbers = new TreeSet<>();

        Pattern patternFilters = Pattern.compile("^filters_operator_rules_[a-zA-Z]*_1_([\\d]+)", Pattern.CASE_INSENSITIVE);
        TreeSet<Integer> filterNumbers = new TreeSet<>();

        for(String s : headers) {
            Matcher matcherPlacements = patternPlacements.matcher(s);
            Matcher matcherFilters = patternFilters.matcher(s);

            if(matcherPlacements.find()) {
                placementNumbers.add(Integer.parseInt(matcherPlacements.group(1)));
            }

            if(matcherFilters.find()) {
                filterNumbers.add(Integer.parseInt(matcherFilters.group(1)));
            }
        }

        try {
            lowestNumPlacement = placementNumbers.first();
            highestNumPlacement = placementNumbers.last();
        }catch (NoSuchElementException nse){
            lowestNumPlacement = 0;
            highestNumPlacement = 0;
        }

        try {
            lowestNumFilter = filterNumbers.first();
            highestNumFilter = filterNumbers.last();
        }catch (NoSuchElementException e){
            lowestNumFilter = 0;
            highestNumFilter = 0;
        }
    }

    protected boolean isEmpty(String str){
        return (str.equals("#N/A") || str.equals("NA") || str.isBlank());
    }

    public void writeFailedRows() throws IOException {
        File csvOutputFile = new File(CSV_FILE_NAME);
        try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
            pw.println("RowNumber,jobGroupId,Reason");
            failedRows.stream()
                    .map(Util::escapeSpecialCharacters)
                    .forEach(pw::println);
        }
    }

}
