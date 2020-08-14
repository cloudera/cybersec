package com.cloudera.cyber.enrichment.lookup;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.typeutils.MapTypeInfo;
import org.apache.flink.table.functions.AggregateFunction;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class EnrichmentAggregator extends AggregateFunction<Map<String,String>, HashMap<String,String>> {

    @Override
    public boolean isDeterministic() {
        return true;
    }

    @Override
    public HashMap<String,String> createAccumulator() {
        return new HashMap<String,String>();
    }

    public void accumulate(HashMap<String,String> accumulator, String field, String type, Map<String, String> entries) {
        if (field == null) return;

        Map<String, String> values = entries.entrySet().stream().collect(Collectors.toMap(
                k -> key(field, type, k.getKey()),
                v -> v.getValue()
        ));
        accumulator.putAll(values);
    }

    public void retract(HashMap<String,String> accumulator, String field, String type, Map<String, String> entries) {
        if(entries == null || entries.size() == 0) {
            accumulator.keySet().stream().filter(p -> p.startsWith(field + "." + type)).forEach(k -> {
                accumulator.remove(k);
            });
        } else {
            entries.entrySet().forEach(entry -> {
                accumulator.remove(key(field, type, entry.getKey()), entry.getValue());
            });
        }
    }

    private String key(String field, String type, String entry) {
        return field + "." + type + "." + entry;
    }

    @Override
    public Map<String,String> getValue(HashMap<String,String> enrichmentAccumulator) {
        return enrichmentAccumulator;
    }

    @Override
    public TypeInformation<Map<String, String>> getResultType() {
        return Types.MAP(Types.STRING, Types.STRING);
    }
}