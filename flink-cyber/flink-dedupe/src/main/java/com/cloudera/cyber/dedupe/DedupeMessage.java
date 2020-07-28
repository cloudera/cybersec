package com.cloudera.cyber.dedupe;

import com.cloudera.cyber.IdentifiedMessage;
import com.cloudera.cyber.Timestamped;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Builder(toBuilder = true)
@Data
public class DedupeMessage implements IdentifiedMessage {
    private UUID id;
    private Map<String, String> fields;
    private long ts;
    private Long startTs;
    private Long count;
    private boolean late = false;
}
