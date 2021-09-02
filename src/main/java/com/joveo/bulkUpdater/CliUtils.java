package com.joveo.bulkUpdater;

import com.joveo.bulkUpdater.model.JoveoException;
import org.apache.commons.cli.*;

public class CliUtils {

    public static final String VALIDATION_ONLY = "v";
    public static final String CLIENT_ID = "c";
    public static final String ADD_JOBGROUP = "a";
    public static final String FILE_INPUT = "i";
    public static final String CREDENTIAL = "p";
    public static final String ENVIRONMENT = "e";

    private static Options options = null;
    private static CommandLine cmd = null;

    public static void setup(String[] args) {
        options = new Options();
        addOptions();
        HelpFormatter formatter = new HelpFormatter();

        try {
            cmd = new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            System.out.println("\nIncorrect options specified");
            formatter.printHelp("bulkUpdater CLI args are", options);
        }

        if (cmd.hasOption("h")) {
            formatter.printHelp("bulkUpdater CLI args are", options);
            System.exit(0);
        }
    }

    public static String getOption(String arg) {
        String val = cmd.getOptionValue(arg);
        if (val == null) {
            throw new JoveoException("Please specify argument for: " + arg);
        }
        return val;
    }

    public static boolean hasOption(String arg) {
        return cmd.hasOption(arg);
    }

    private static void addOptions() {
        options.addOption("h", "help", false, "shows cli commands");
        options.addOption(Option.builder(CLIENT_ID).argName("clientId").longOpt("client-id").desc("specify client id for update").hasArg().build());
        options.addOption(Option.builder(FILE_INPUT).argName("filename").longOpt("file-input").desc("specify relative file path for input").hasArg().build());
        options.addOption(Option.builder(CREDENTIAL).argName("username:password").longOpt("credential").desc("specify username:password to connect").hasArg().build());
        options.addOption(Option.builder(ENVIRONMENT).argName("env").longOpt("environment").desc("specify joveo environment to connect").hasArg().build());
        options.addOption(VALIDATION_ONLY, "validation-only", false, "if specified, only runs validation stage and not update");
        options.addOption(ADD_JOBGROUP, "add-jobgroup", false, "if specified, adds a new job group");
    }
}
