package com.cloudera.cyber.caracal;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class SplitConfig {

    private String topic;
    private String splitPath;
    private String headerPath;
    private String timestampField;
}
