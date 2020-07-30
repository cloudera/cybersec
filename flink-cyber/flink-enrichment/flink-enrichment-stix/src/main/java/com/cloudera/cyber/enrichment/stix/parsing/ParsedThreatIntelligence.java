package com.cloudera.cyber.enrichment.stix.parsing;

import com.cloudera.cyber.ThreatIntelligence;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ParsedThreatIntelligence {
    UUID id;
    ThreatIntelligence threatIntelligence;
    String source;
}
