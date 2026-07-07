package com.cloudera.cyber.enrichment.geocode.database.types;

import java.util.Map;

public class ValueConversions {

    /**
     * Return null if the string is null, blank or empty.
     *
     * @param str string to convert
     * @return str if str is not null or blank
     */
    public static String convertEmptyToNull(String str) {
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

    public static <T> T safeLookup(Map<String, Object> map, String key, Class<T> targetType) {
        if (map != null && key != null && targetType != null) {
            Object value = map.get(key);
            if (value != null) {
                return targetType.isInstance(value) ? targetType.cast(value) : null;
            }
        }
        return null;
    }

    public static Long extractIpinfoAsnNumber(String asnAsString) {
        if (asnAsString != null && asnAsString.matches("AS\\d+")) {
            return Long.valueOf(asnAsString.substring(2));
        }
        return null;
    }
}


