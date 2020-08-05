package com.cloudera.cyber;

public interface IdentifiedMessage extends Timestamped{
    String getId();
    long getTs();
}
