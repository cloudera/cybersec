package com.cloudera.cyber.indexing.hive;

import lombok.Builder;
import lombok.Data;
import org.apache.flink.types.Row;

@Data
@Builder
public class ErrorRow {
    Row row;
    Throwable exception;

    public String toString() {
        return String.format("Error Row: %s, Exception %s", row.toString(), exception.getMessage());
    }
}
