package com.cloudera.cyber.indexing.hive;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FilterMapFunctionTest {

    private static Map<String, String> extensions() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("geo_country", "US");
        map.put("geo.city", "Durham");
        map.put("hbase_enrichment", "value");
        map.put("keep_me", "value");
        map.put("FieldToIgnore", "value");
        return map;
    }

    @Test
    void shouldRemoveExactFieldsCaseInsensitively() {
        FilterMapFunction function = new FilterMapFunction(Set.of("fieldtoignore"), Set.of());

        Map<String, String> result = function.eval(extensions());

        assertThat(result).doesNotContainKeys("FieldToIgnore");
        assertThat(result).containsKeys("geo_country", "geo.city", "hbase_enrichment", "keep_me");
    }

    @Test
    void shouldRemoveFieldsMatchingPrefix() {
        FilterMapFunction function = new FilterMapFunction(Set.of(), Set.of("geo_", "geo."));

        Map<String, String> result = function.eval(extensions());

        assertThat(result).containsOnlyKeys("hbase_enrichment", "keep_me", "FieldToIgnore");
    }

    @Test
    void shouldRemoveFieldsMatchingEitherExactNameOrPrefix() {
        FilterMapFunction function = new FilterMapFunction(Set.of("keep_me"), Set.of("geo_", "geo."));

        Map<String, String> result = function.eval(extensions());

        assertThat(result).containsOnlyKeys("hbase_enrichment", "FieldToIgnore");
    }

    @Test
    void shouldReturnMapUnchangedWhenNothingToIgnore() {
        FilterMapFunction function = new FilterMapFunction(Set.of(), Set.of());

        assertThat(function.eval(extensions())).isEqualTo(extensions());
    }

    @Test
    void shouldReturnNullForNullMap() {
        FilterMapFunction function = new FilterMapFunction(Set.of("field"), Set.of("prefix"));

        assertThat(function.eval(null)).isNull();
    }
}
