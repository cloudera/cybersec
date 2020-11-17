package com.cloudera.cyber.enrichment.hbase;

import org.apache.flink.table.functions.ScalarFunction;

import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;


/**
 * Add the results of enrichments to a map
 *
 * The will merge two or more maps together. It will not overwrite values from the later maps
 */
public class MapMergeFunction extends ScalarFunction {
    public MapMergeFunction() {
        super();
    }

    public Map<String, String> eval(Map<String, String>... merge) {
        return Stream.of(merge)
                .map(s -> s.entrySet().stream())
                .flatMap(s -> s)
                .collect(toMap(
                        k -> k.getKey(),
                        v -> v.getValue(),
                        (o, n) -> o
                ));
    }

    @Override
    public boolean isDeterministic() {
        return true;
    }
}
