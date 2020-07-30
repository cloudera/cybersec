package com.cloudera.cyber.enrichment.stix;

import com.cloudera.cyber.ThreatIntelligence;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;

@Data
@Builder
public class ThreatIntelligenceIndex extends HashMap<String, ThreatIntelligence> {

}
