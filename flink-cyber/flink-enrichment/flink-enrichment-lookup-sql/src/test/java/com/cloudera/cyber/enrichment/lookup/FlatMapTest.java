package com.cloudera.cyber.enrichment.lookup;

import org.hamcrest.collection.IsMapWithSize;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;

public class FlatMapTest {

    @Test
    public void testFlatMap() {
        HashMap<String, Object> example = new HashMap<String, Object>() {{
            put("a", new HashMap<String, Object>() {{
                put("a.1", "value-a.1");
                put("a.2", new HashMap<String, Object>() {{
                    put("a.2.1", "value-a.2.1");
                }});

            }});
            put("b", "value-b");
        }};

        Map<String, Object> out = Flatten.flatten(example);
        assertThat("", out, IsMapWithSize.aMapWithSize(3));
    }
}
