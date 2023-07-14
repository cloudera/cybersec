package com.cloudera.parserchains.queryservice.consumer;

import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.BODY_PARAM;
import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.HEADERS_PARAM;
import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.STATUS_PARAM;
import com.cloudera.parserchains.core.utils.JSONUtils;
import com.cloudera.parserchains.queryservice.config.kafka.ClouderaKafkaProperties;
import com.cloudera.parserchains.queryservice.handler.InterconnectHandler;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InterconnectConsumer {

  //Used in Kafka annotations
  @SuppressWarnings("unused")
  private final ClouderaKafkaProperties kafkaProperties;
  private final InterconnectHandler interconnectHandler;

  @KafkaListener(topics = "#{kafkaProperties.getRequestTopic()}")
  @SendTo({"#{kafkaProperties.getReplyTopic()}"})
  public String replyTest(ConsumerRecord<String, String> payload) throws IOException {
    final ResponseEntity<?> response = interconnectHandler.handle(payload.key(), payload.value());

    final Map<String, Object> result = new HashMap<>();
    result.put(BODY_PARAM, response.getBody());
    result.put(HEADERS_PARAM, response.getHeaders());
    result.put(STATUS_PARAM, response.getStatusCode());

    return JSONUtils.INSTANCE.toJSON(result, false);
  }

}
