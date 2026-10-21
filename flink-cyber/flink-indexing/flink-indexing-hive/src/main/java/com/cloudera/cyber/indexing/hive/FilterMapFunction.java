package com.cloudera.cyber.indexing.hive;

import org.apache.flink.table.functions.ScalarFunction;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class FilterMapFunction extends ScalarFunction {

    private final Set<String> ignoreFields;
    private final Set<String> ignorePrefixes;

    public FilterMapFunction(Set<String> ignoreFields, Set<String> ignorePrefixes) {
        this.ignoreFields = ignoreFields == null ? Collections.emptySet() : lowerCase(ignoreFields);
        this.ignorePrefixes = ignorePrefixes == null ? Collections.emptySet() : lowerCase(ignorePrefixes);
    }

    private static Set<String> lowerCase(Set<String> values) {
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    public Map<String, String> eval(Map<String, String> map) {
        if (map == null || (ignoreFields.isEmpty() && ignorePrefixes.isEmpty())) {
            return map;
        }
        return map.entrySet().stream()
                .filter(e -> e.getKey() != null && !shouldIgnore(e.getKey().toLowerCase()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private boolean shouldIgnore(String key) {
        return ignoreFields.contains(key) || ignorePrefixes.stream().anyMatch(key::startsWith);
    }
}
