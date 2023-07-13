package com.cloudera.parserchains.queryservice.service;

import com.cloudera.parserchains.queryservice.config.kafka.ClouderaReplyingKafkaTemplate;
import com.cloudera.parserchains.queryservice.model.enums.KafkaMessageType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.requestreply.KafkaReplyTimeoutException;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaService {

  @Qualifier("kafkaTemplatePool")
  private final Map<String, ClouderaReplyingKafkaTemplate<String, String, String>> kafkaTemplatePool;

  public String sendWithReply(String clusterId, KafkaMessageType messageType, String message) {
    final ClouderaReplyingKafkaTemplate<String, String, String> kafkaTemplate = kafkaTemplatePool.get(clusterId);
    if (kafkaTemplate == null){
      throw new RuntimeException("Cluster not found!");
    }

    ProducerRecord<String, String> record = new ProducerRecord<>(kafkaTemplate.getRequestTopic(),
        messageType.name(), message);
    RequestReplyFuture<String, String, String> replyFuture = kafkaTemplate.sendAndReceive(record);
    try {
      ConsumerRecord<String, String> consumerRecord = replyFuture.get(10, TimeUnit.SECONDS);
      if (consumerRecord == null) {
        throw new KafkaException("Got no reply");
      }
      return consumerRecord.value();
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      // Handle exception
      throw new KafkaReplyTimeoutException("Timeout while waiting for reply");
    }
  }

}
