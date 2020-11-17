package com.cloudera.cyber.enrichment.hbase;

import java.util.HashMap;
import java.util.Map;

public class TestHbaseEnrichmentFunction extends HbaseEnrichmentFunction {
    @Override
    public Map<String, String> eval(long timestamp, String type, String key) {
        return new HashMap<String,String>() {{ put("test", "values"); }};
    }
}
