package com.cloudera.cyber.dedupe.impl;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.dedupe.DedupeMessage;
import org.apache.flink.api.common.functions.RichMapFunction;

import java.util.List;
import java.util.stream.Collectors;

public class CreateKeyFromMessage extends RichMapFunction<Message, DedupeMessage> {
    List<String> key;

    public CreateKeyFromMessage(List<String> key) {
        this.key = key;
    }

    @Override
    public DedupeMessage map(Message message) {
        return DedupeMessage.builder()
                .count(1L)
                .fields(key.stream()
                        .collect(Collectors.toMap(
                                f -> f,
                                f->message.get(f).toString()))
                )
                .ts(message.getTs())
                .id(message.getId())
                .build();
    }
}
