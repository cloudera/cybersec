package com.cloudera.cyber.rules;

import com.cloudera.cyber.Message;

import java.util.Map;
import java.util.UUID;

public interface DynamicRule<T> {
    boolean isEnabled();

    int getOrder();

    String getId();

    T withId(String uuid);
    T withEnabled(boolean enabled);

    Map<String, Object> apply(Message message);
}
