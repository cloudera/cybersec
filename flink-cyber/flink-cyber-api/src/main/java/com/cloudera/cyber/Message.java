package com.cloudera.cyber;

import lombok.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class Message implements IdentifiedMessage, Timestamped {
    protected UUID id;
    protected long ts;
    protected String originalSource;
    protected Map<String, Object> fields;

    public Object get(String field) {
        return fields.get(field);
    }
    public void set(String field, Object value) {
        fields.put(field, value);
    }


    public static class MessageBuilder {
        private UUID id = UUID.randomUUID();
        private Long ts = Instant.now().toEpochMilli();
        private Map<String, Object> fields = new HashMap<>();

        public MessageBuilder put(String key, Object value) {
            this.fields.put(key, value);
            return this;
        }
    }
}

