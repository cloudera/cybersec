package com.cloudera.cyber.enrichment.lookup.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrichmentConfig implements Serializable {
    EnrichmentKind kind;
    String source;
    String type;
    List<EnrichmentField> fields;
}
