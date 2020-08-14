package com.cloudera.cyber.enrichment.lookup;

import lombok.Data;
import lombok.Builder;

import java.io.Serializable;

@Builder
@Data
public class EnrichmentKey implements Serializable  {
    private String type;
    private String key;

}
