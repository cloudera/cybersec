package com.cloudera.cyber.enrichment.hbase;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LookupKey {
    private byte[] cf;
    private byte[] key;
}
