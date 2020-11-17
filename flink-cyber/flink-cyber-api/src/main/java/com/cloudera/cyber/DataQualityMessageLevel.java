package com.cloudera.cyber;

import org.apache.avro.Schema;

import java.util.Arrays;

import static java.util.stream.Collectors.toList;

public enum DataQualityMessageLevel {
    ERROR, WARNING, INFO;

    private static Schema schema = Schema.createEnum(DataQualityMessageLevel.class.getName(),
            "",
            DataQualityMessageLevel.class.getPackage().getName(),
            Arrays.stream(values()).map(e -> e.toString()).collect(toList()));

    public static Schema getSchema() {
        return schema;
    }
}
