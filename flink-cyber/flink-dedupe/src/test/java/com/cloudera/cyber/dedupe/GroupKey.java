package com.cloudera.cyber.dedupe;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
class GroupKey implements Comparable<GroupKey> {
    private String a;
    private String b;
    private Long ts;

    @Override
    public int compareTo(GroupKey o) {
        return Math.toIntExact(o.ts - ts);
    }
}
