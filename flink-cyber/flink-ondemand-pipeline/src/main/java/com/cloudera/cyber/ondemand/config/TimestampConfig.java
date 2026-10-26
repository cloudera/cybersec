package com.cloudera.cyber.ondemand.config;

import org.apache.flink.api.java.utils.ParameterTool;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class TimestampConfig {

    private static final String EXPECTED_DATE_FORMAT_STRING = "yyyy-MM-dd HH:mm:ss.SSS";
    private static final DateTimeFormatter EXPECTED_SETTING_TIMESTAMP_FORMAT =  DateTimeFormatter.ofPattern(EXPECTED_DATE_FORMAT_STRING);

    public static LocalDateTime getConfiguration(ParameterTool params, String paramKey) {
        String timeAsString = params.getRequired(paramKey);
        try {
            return LocalDateTime.parse(timeAsString, EXPECTED_SETTING_TIMESTAMP_FORMAT);
        } catch (DateTimeParseException parseException) {
            throw new RuntimeException(String.format("Configuration '%s' value '%s' does not match the expected date format '%s'", paramKey, timeAsString, EXPECTED_DATE_FORMAT_STRING), parseException);
        }
    }
}
