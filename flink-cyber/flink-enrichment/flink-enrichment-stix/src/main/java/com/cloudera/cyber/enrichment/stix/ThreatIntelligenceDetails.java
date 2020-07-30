package com.cloudera.cyber.enrichment.stix;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ThreatIntelligenceDetails {
    private UUID id;
    private String stixSource;
}
