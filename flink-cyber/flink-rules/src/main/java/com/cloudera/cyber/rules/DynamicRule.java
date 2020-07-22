package com.cloudera.cyber.rules;

import com.cloudera.cyber.Message;

import java.util.Map;
import java.util.UUID;

public interface DynamicRule<T> {
    boolean isEnabled();

    int getOrder();

    UUID getId();

    T withId(UUID uuid);
    T withEnabled(boolean enabled);

    Map<String, Object> apply(Message message);
}
