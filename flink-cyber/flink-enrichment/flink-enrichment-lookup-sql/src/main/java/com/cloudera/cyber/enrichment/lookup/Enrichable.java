package com.cloudera.cyber.enrichment.lookup;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Enrichable implements Serializable {
    private long ts;
    private String id;
    private String field;
    private String fieldValue;
    private String enrichmentType;
    private String lookupKey;
}
