package com.joveo.bulkUpdater.util;

public class Util {

    public static String escapeSpecialCharacters(String data) {
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "");
            data = data.replace("\'", "");
            escapedData = data;
        }
        return escapedData;
    }
}
