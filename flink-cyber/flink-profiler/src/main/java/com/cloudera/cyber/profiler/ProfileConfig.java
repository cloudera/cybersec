package com.cloudera.cyber.profiler;


import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Data
@Builder
public class ProfileConfig {
    private String name;
    private String source;
    private String entity;
    private String filter;
    private Map<String, String> fields;
    private Integer interval;
    private TimeUnit intervalUnit;
    private ProfileMode mode;
    private Long latenessTolerance;
}