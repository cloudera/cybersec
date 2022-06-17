package com.cloudera.cyber.profiler.sql.catalog;

public class IllegalSchemaException extends RuntimeException {
    public IllegalSchemaException(String message) {
        super(message);
    }
}
