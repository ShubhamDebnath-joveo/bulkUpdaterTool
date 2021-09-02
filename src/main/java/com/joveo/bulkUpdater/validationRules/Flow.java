package com.joveo.bulkUpdater.validationRules;

import com.joveo.eqrtestsdk.core.entities.Driver;
import org.apache.commons.csv.CSVRecord;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Flow {

    protected int lowestNumPlacement = -1;
    protected int highestNumPlacement = -1;
    protected Set<String> headers = null;
    protected Driver driver;

    public Flow(Set<String> headers, Driver driver){
        this.headers = headers;
        this.driver = driver;
        setNumPlacement();
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
}
