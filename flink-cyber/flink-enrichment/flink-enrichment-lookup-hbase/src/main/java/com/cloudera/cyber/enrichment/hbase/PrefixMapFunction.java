package com.cloudera.cyber.enrichment.hbase;

import org.apache.flink.table.functions.ScalarFunction;

import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class PrefixMapFunction extends ScalarFunction {
    public Map<String, String> eval(Map<String, String> map, String prefix) {
        return map.entrySet().stream()
                .collect(toMap(
                        k -> prefix + k.getKey(),
                        v -> v.getValue())
                );
    }
}
