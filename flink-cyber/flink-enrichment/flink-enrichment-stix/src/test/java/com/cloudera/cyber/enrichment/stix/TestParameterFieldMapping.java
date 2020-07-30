package com.cloudera.cyber.enrichment.stix;

import org.apache.flink.api.java.utils.ParameterTool;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.IsIterableContaining.hasItem;

public class TestParameterFieldMapping {

    @Test
    public void testParameterMapping() {
        Map<String, String> map = new HashMap<String, String>() {{
            put("threatIntelligence.ip", "AddressObj:ipv4_addr");
            put("threatIntelligence.field.with.dots.1", "AddressObj:ipv4_addr");
            put("threatIntelligence.multi.1", "test1");
            put("threatIntelligence.multi.2", "test2");
        }};
        ParameterTool params = ParameterTool.fromMap(map);

        Map<String, List<String>> output = StixJob.getFieldsMappings(params);

        assertThat("Has IP field", output, hasEntry("ip", Arrays.asList("AddressObj:ipv4_addr")));
        assertThat("Has Dotted field", output, hasEntry("field.with.dots", Arrays.asList("AddressObj:ipv4_addr")));
        assertThat("Has multi field", output.get("multi"), hasItem(equalTo("test1")));
        assertThat("Has multi field", output.get("multi"), hasItem(equalTo("test2")));

    }
}
