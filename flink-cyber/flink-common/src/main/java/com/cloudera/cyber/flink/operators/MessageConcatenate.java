package com.cloudera.cyber.flink.operators;

import com.cloudera.cyber.GroupedMessage;
import com.cloudera.cyber.Message;
import org.apache.flink.api.common.functions.AggregateFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MessageConcatenate implements AggregateFunction<Message, List<Message>, GroupedMessage> {

    @Override
    public List<Message> createAccumulator() {
        return new ArrayList<>();
    }

    @Override
    public List<Message> merge(List<Message> messageList, List<Message> acc1) {
        return Stream.concat(messageList.stream(), acc1.stream()).collect(Collectors.toList());
    }

    @Override
    public GroupedMessage getResult(List<Message> messageList) {
        return GroupedMessage.builder().messages(messageList).build();
    }

    @Override
    public List<Message> add(Message message, List<Message> messageList) {
        messageList.add(message);
        return messageList;
    }
}
