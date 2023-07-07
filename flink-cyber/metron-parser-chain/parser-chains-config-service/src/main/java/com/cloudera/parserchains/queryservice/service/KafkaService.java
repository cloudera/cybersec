package com.cloudera.parserchains.queryservice.service;

import com.cloudera.parserchains.queryservice.config.KafkaConsumerConfig;
import com.cloudera.parserchains.queryservice.model.enums.KafkaMessageType;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.requestreply.KafkaReplyTimeoutException;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaService {

  private final ReplyingKafkaTemplate<String, String, String> replyingKafkaTemplate;
  private final KafkaConsumerConfig kafkaConsumerConfig;

  public String sendWithReply(String clusterId, KafkaMessageType messageType, String message) {
    ProducerRecord<String, String> record = new ProducerRecord<>(kafkaConsumerConfig.getRequestTopic(),
        formatKafkaKey(clusterId, messageType), message);
    RequestReplyFuture<String, String, String> replyFuture = replyingKafkaTemplate.sendAndReceive(record);
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

  private static String formatKafkaKey(String clusterId, KafkaMessageType messageType){
    return String.format("%s;%s", clusterId, messageType);
  }

}
