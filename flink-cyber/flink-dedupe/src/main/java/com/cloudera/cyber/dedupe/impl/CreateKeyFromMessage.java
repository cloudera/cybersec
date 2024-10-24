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

package com.cloudera.cyber.dedupe.impl;

import com.cloudera.cyber.DedupeMessage;
import com.cloudera.cyber.Message;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.flink.api.common.functions.RichMapFunction;

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
                                             f -> message.get(f).toString()))
                            )
                            .ts(message.getTs())
                            .build();
    }
}
