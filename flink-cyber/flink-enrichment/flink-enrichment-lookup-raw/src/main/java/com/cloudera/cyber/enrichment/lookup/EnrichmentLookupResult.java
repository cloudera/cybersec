package com.cloudera.cyber.enrichment.lookup;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class EnrichmentLookupResult {
    String messageId;
    String field;
    String type;
    String key;
    Map<String, String> values;
}
