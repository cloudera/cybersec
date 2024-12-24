package com.cloudera.cyber.indexing.hive;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.flink.table.functions.ScalarFunction;

public class FilterMapFunction extends ScalarFunction {

    public Map<String, String> eval(Map<String, String> map, String... keys) {
        if (keys.length < 1) {
            return map;
        }
        final Set<String> fieldsToIgnore = Arrays.stream(keys).collect(Collectors.toSet());
        return map.entrySet().stream()
              .filter(e -> e.getKey() != null && !fieldsToIgnore.contains(e.getKey().toLowerCase()))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
