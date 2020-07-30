package com.cloudera.parserchains.core;

public class Validator {

    private Validator() {
        // do not instantiate this class
    }

    /**
     * Throws an exception if a value does not match a regular expression.
     * @param value The value that must match.
     * @param regex The regular expression that defines a valid value.
     * @param entityName The name of the entity being validated.
     * @throws IllegalArgumentException If the value is invalid.
     */
    public static void mustMatch(String value, Regex regex, String entityName) throws IllegalArgumentException {
        if(!regex.matches(value)) {
            throw new IllegalArgumentException(String.format("Invalid %s=%s", entityName, value));
        }
    }
}
