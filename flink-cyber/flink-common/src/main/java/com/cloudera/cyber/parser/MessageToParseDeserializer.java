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

