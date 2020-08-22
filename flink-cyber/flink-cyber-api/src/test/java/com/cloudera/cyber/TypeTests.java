package com.cloudera.cyber;

import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.junit.Test;

import java.util.Map;

public class TypeTests {

    @Test
    public void testTypes() {
        TypeInformation<EnrichmentEntry> types = TypeInformation.of(EnrichmentEntry.class);

        TypeInformation<Map<String, String>> mapType = TypeInformation.of(new TypeHint<Map<String, String>>() {
        });

        System.out.println(types);
    }
}
