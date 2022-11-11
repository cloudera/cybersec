package com.cloudera.cyber.hbase;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;

import java.util.Map;

@Getter
@SuperBuilder
@EqualsAndHashCode
public abstract class LookupKey {
    private String cf;
    private String tableName;
    private String key;

    public abstract Get toGet();
    public abstract Map<String, Object> resultToMap(Result hbaseResult);
}