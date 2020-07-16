package com.cloudera.cyber;

import lombok.*;

import java.util.Map;
import java.util.UUID;

@Getter
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class Message implements IdentifiedMessage {
    protected UUID id;
    protected Long ts;
    protected String originalSource;
    protected Map<String, Object> fields;

    public Object get(String field) {
        return fields.get(field);
    }
    public void set(String field, Object value) {
        fields.put(field, value);
    }
}

