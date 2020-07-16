package com.cloudera.cyber;

import java.util.UUID;

public interface IdentifiedMessage {
    UUID getId();
    Long getTs();
}
