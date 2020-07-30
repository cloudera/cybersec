package com.cloudera.cyber;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class ThreatIntelligence implements IdentifiedMessage {
    @Builder.Default @NonNull private UUID id = UUID.randomUUID();
    private long ts;
    @NonNull private String observable;
    @NonNull private String observableType;
    private String stixReference;

    private Map<String,String> fields;
}
