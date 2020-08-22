package com.cloudera.cyber.enrichment.lookup;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Builder
@Data
public class EnrichmentKey implements Serializable  {
    private String type;
    private String key;
}
