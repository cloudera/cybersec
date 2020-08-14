package com.cloudera.cyber.enrichment.lookup.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrichmentField implements Serializable {
    private String enrichmentType;
    private String name;
}
