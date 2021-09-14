package com.joveo.bulkUpdater.util;

import com.joveo.eqrtestsdk.models.Freq;
import com.joveo.eqrtestsdk.models.RuleOperator;

import java.util.Arrays;
import java.util.List;

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

    public static <T extends Enum<?>> T searchEnum(Class<T> enumeration,
                                                   String search) {
        for (T each : enumeration.getEnumConstants()) {
            if (each.name().compareToIgnoreCase(search) == 0) {
                return each;
            }
        }
        return null;
    }
}
