package com.cloudera.parserchains.queryservice.consumer;

import com.cloudera.parserchains.queryservice.config.kafka.ClouderaKafkaProperties;
import com.cloudera.parserchains.queryservice.handler.InterconnectHandler;
import com.cloudera.parserchains.queryservice.model.enums.KafkaMessageType;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InterconnectConsumer {

  private final ClouderaKafkaProperties kafkaProperties;
  private final InterconnectHandler interconnectHandler;

  @KafkaListener(topics = "#{kafkaProperties.getRequestTopic()}")
  @SendTo({"#{kafkaProperties.getReplyTopic()}"})
  public String replyTest(ConsumerRecord<String, String> payload) {
    return interconnectHandler.handle(payload.key(), payload.value());
  }

}
