package com.cloudera.cyber;

import java.util.regex.Pattern;

public final class ValidateUtils {

    private static final Pattern PHOENIX_NAME_REGEXP = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]+");
    private static final Pattern UNDERSCORE_PATTERN =  Pattern.compile("_+");

    private ValidateUtils() {
    }

    public static void validatePhoenixName(String value, String parameter) {
        if ( !PHOENIX_NAME_REGEXP.matcher(value).matches() || UNDERSCORE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(String.format("Invalid value %s for parameter '%s'. It can only contain alphanumerics or underscore(a-z, A-Z, 0-9, _)", value, parameter));
        }
    }

}
