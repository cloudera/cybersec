package com.cloudera.cyber.profiler.sql;

public class IllegalSchemaException extends RuntimeException {
    public IllegalSchemaException(String message) {
        super(message);
    }
}
