/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

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
