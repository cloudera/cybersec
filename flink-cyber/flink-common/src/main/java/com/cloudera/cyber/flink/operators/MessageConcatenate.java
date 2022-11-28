/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

package com.cloudera.cyber.flink.operators;

import com.cloudera.cyber.GroupedMessage;
import com.cloudera.cyber.Message;
import org.apache.flink.api.common.functions.AggregateFunction;

import java.util.ArrayList;
import java.util.List;
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
