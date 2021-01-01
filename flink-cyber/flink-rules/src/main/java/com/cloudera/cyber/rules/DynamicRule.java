package com.cloudera.cyber.rules;

import com.cloudera.cyber.Message;

import java.util.Map;
import java.util.UUID;

public interface DynamicRule<T> {
    boolean isEnabled();

    int getOrder();

    String getId();

    int getVersion();

    T withId(String uuid);
    T withEnabled(boolean enabled);
    T withVersion(int version);

    Map<String, Object> apply(Message message);
}
