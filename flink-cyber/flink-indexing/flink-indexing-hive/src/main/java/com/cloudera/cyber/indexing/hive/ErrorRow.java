package com.cloudera.cyber.indexing.hive;

import lombok.Data;
import lombok.Builder;
import org.apache.flink.types.Row;

@Data
@Builder
public class ErrorRow {
    Row row;
    Throwable exception;
}
