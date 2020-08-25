package com.cloudera.cyber.indexing;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Data
@Slf4j
@Builder
public class IndexEntry {
    private String index;
    private String id;
    private long timestamp;
    private Map<String, String> fields;
}
