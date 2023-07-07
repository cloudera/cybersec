package com.cloudera.parserchains.queryservice.consumer;

import com.cloudera.parserchains.queryservice.config.KafkaConsumerConfig;
import com.cloudera.parserchains.queryservice.model.enums.KafkaMessageType;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InterconnectConsumer {

  private final KafkaConsumerConfig kafkaConsumerConfig;

  @KafkaListener(topics = "#{kafkaConsumerConfig.getRequestTopic()}")
  @SendTo({"#{kafkaConsumerConfig.getReplyTopic()}"})
  public String replyTest(ConsumerRecord<String, String> payload) {
    final String[] keySplit = payload.key().split(";");
    final String clusterId = keySplit[0];
    if (!getClusterId().equals(clusterId)){
      return null;
    }

    final String messageType = keySplit[1];
    final String value = payload.value();

    switch (KafkaMessageType.fromString(messageType)) {
      case PARSER:
        return "Processed parser request: " + value;
      case CLUSTER:
        return "Processed cluster request: " + value;
      default:
        return String.format("Processed unknown request (%s): %s", messageType, value);
    }
  }

  private String getClusterId(){
    return "clusterId";
  }

}
