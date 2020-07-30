package com.cloudera.cyber.libs.hostnames;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class NsLookupRecord {
    private String type;
    private String result;
    private String name;
}