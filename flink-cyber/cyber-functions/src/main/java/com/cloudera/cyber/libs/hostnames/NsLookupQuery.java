package com.cloudera.cyber.libs.hostnames;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Builder
@Data
@EqualsAndHashCode
public class NsLookupQuery {
    private String query;
    private int type;
}
