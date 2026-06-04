package com.cloudera.cyber.enrichment.geocode.database.types.geo;

public class ValueConversions {

    /**
     * Return null if the string is null, blank or empty.
     *
     * @param str string to convert
     * @return str if str is not null or blank
     */
    protected static String convertEmptyToNull(String str) {
        return (str == null || str.isBlank()) ? null : str;
    }

    /**
     * Converts null to empty string
     *
     * @param raw The raw object
     * @return Empty string if null, or the String value if not
     */
    public static String convertNullToEmptyString(Object raw) {
        return raw == null ? "" : String.valueOf(raw);
    }
}
