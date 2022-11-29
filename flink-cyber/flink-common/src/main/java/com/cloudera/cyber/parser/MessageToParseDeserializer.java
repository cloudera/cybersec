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

package com.cloudera.cyber.parser;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.nio.charset.StandardCharsets;

public class MessageToParseDeserializer implements KafkaDeserializationSchema<MessageToParse> {

    @Override
    public boolean isEndOfStream(MessageToParse messageToParse) {
        return false;
    }

    @Override
    public MessageToParse deserialize(ConsumerRecord<byte[], byte[]> consumerRecord) {
        return MessageToParse.builder()
                .originalBytes(consumerRecord.value())
                .topic(consumerRecord.topic())
                .offset(consumerRecord.offset())
                .partition(consumerRecord.partition())
                .key(consumerRecord.key())
                .build();
    }

    @Override
    public TypeInformation<MessageToParse> getProducedType() {
        return TypeInformation.of(MessageToParse.class);
    }
}

