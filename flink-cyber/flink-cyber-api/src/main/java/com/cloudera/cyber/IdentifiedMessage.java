package com.cloudera.cyber;

import java.util.UUID;

public interface IdentifiedMessage extends Timestamped{
    UUID getId();
    long getTs();
}
