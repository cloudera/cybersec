package com.cloudera.parserchains.queryservice.config.kafka;

import lombok.Getter;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.GenericMessageListenerContainer;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;

public class ClouderaReplyingKafkaTemplate<K, V, R> extends ReplyingKafkaTemplate<K, V, R> {

    @Getter
    private String requestTopic;

    public ClouderaReplyingKafkaTemplate(ProducerFactory<K, V> producerFactory,
                                         GenericMessageListenerContainer<K, R> replyContainer, String requestTopic) {
        this(producerFactory, replyContainer);
        this.requestTopic = requestTopic;
    }

    public ClouderaReplyingKafkaTemplate(ProducerFactory<K, V> producerFactory,
                                         GenericMessageListenerContainer<K, R> replyContainer, String requestTopic,
                                         boolean autoFlush) {
        this(producerFactory, replyContainer, autoFlush);
        this.requestTopic = requestTopic;
    }

    private ClouderaReplyingKafkaTemplate(ProducerFactory<K, V> producerFactory,
                                          GenericMessageListenerContainer<K, R> replyContainer) {
        super(producerFactory, replyContainer);
    }

    private ClouderaReplyingKafkaTemplate(ProducerFactory<K, V> producerFactory,
                                          GenericMessageListenerContainer<K, R> replyContainer, boolean autoFlush) {
        super(producerFactory, replyContainer, autoFlush);
    }
}
