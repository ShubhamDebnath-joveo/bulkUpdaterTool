package com.joveo.bulkUpdater;

import com.joveo.bulkUpdater.model.JoveoException;
import com.joveo.bulkUpdater.model.ValidationResult;
import com.joveo.bulkUpdater.util.Util;
import com.joveo.bulkUpdater.validationRules.*;
import com.joveo.eqrtestsdk.core.entities.Driver;
import com.joveo.eqrtestsdk.exception.MojoException;
import com.joveo.eqrtestsdk.models.JobGroupDto;
import com.joveo.eqrtestsdk.models.JoveoEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// check first non-empty row for header
// if edit mode , then only jobgroupId mandatory
// name may be duplicate, but corresponding jobGroupId cant be
// separate validate and run methods (as an interface) for create and edit mode
// under dry-run mode, only run validate
// 1 class for reading and building object
// 1 interface for edit or create mode, it will have validate and run function, in edit mode only validate

// 2 document level rules
// all required columns present or not
// each placement is present in group of 7 fields
@Slf4j
public class ReadUtil {

    private static int lowestNumPlacement = -1;
    private static int highestNumPlacement = -1;
    private static final String CSV_FILE_NAME = "rows_validation_error.csv";

    public static void readFile() {
        boolean validationOnly = CliUtils.hasOption(CliUtils.VALIDATION_ONLY);
        File file = new File(CliUtils.getOption(CliUtils.FILE_INPUT));
        Reader in = null;
        String[] credentials = CliUtils.getOption(CliUtils.CREDENTIAL).split(":");
        String username = credentials[0];
        String password = credentials[1];
        String env = CliUtils.getOption(CliUtils.ENVIRONMENT).substring(0, 1).toUpperCase() + CliUtils.getOption(CliUtils.ENVIRONMENT).substring(1).toLowerCase();
        JoveoEnvironment environment = JoveoEnvironment.valueOf(env);

        List<ValidationResult> errorRows = new ArrayList<>();
        List<ValidationResult> validRows = new ArrayList<>();

        try {
            in = new FileReader(file);
            List<CSVRecord> records = CSVFormat.DEFAULT.builder().setIgnoreEmptyLines(true).setHeader().build().parse(in).getRecords();
            Driver driver = Driver.start(username, password, environment);

            CSVRecord firstRecord = records.get(0);
            Flow flow = FlowFactory.getFLow(firstRecord.toMap().keySet(), driver);
            FieldLevelRule rules = flow.buildRules();

            records.stream().map(record -> rules.validate(record)).forEach(
                    result -> {
                        if(!result.isValid()){
                            log.info(result.toString());
                            errorRows.add(result);
                        }else {
                            if(!validationOnly){
                                validRows.add(result);
                            }
                        }
                    }
            );

            validRows.forEach(row -> flow.processRecord(row.getRecord()));
            flow.writeFailedRows();


            File csvOutputFile = new File(CSV_FILE_NAME);
            try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
                pw.println("RowNumber,jobGroupId,Reason");
                errorRows.stream()
                        .map(d -> Util.escapeSpecialCharacters(d.toString()))
                        .forEach(pw::println);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MojoException e) {
            e.printStackTrace();
        }
    }

}