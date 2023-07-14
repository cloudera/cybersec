package com.cloudera.parserchains.queryservice.service;

import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.BODY_PARAM;
import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.HEADERS_PARAM;
import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.STATUS_PARAM;
import com.cloudera.parserchains.core.utils.JSONUtils;
import com.cloudera.parserchains.queryservice.config.kafka.ClouderaReplyingKafkaTemplate;
import com.cloudera.parserchains.queryservice.model.enums.KafkaMessageType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.requestreply.KafkaReplyTimeoutException;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaService {

  private static final ObjectMapper MAPPER = JSONUtils.INSTANCE.getMapper();
  private static final ObjectReader HEADERS_READER = MAPPER.readerFor(HttpHeaders.class);

  @Qualifier("kafkaTemplatePool")
  private final Map<String, ClouderaReplyingKafkaTemplate<String, String, String>> kafkaTemplatePool;


  public <T> ResponseEntity<T> sendWithReply(KafkaMessageType messageType, String clusterId, Object body,
      ObjectReader responseReader)
      throws JsonProcessingException {
    final ClouderaReplyingKafkaTemplate<String, String, String> kafkaTemplate = kafkaTemplatePool.get(clusterId);
    if (kafkaTemplate == null) {
      throw new RuntimeException("Cluster not found!");
    }

    final String jsonBody = JSONUtils.INSTANCE.toJSON(body, false);

    ProducerRecord<String, String> record = new ProducerRecord<>(kafkaTemplate.getRequestTopic(),
        messageType.name(), jsonBody);
    RequestReplyFuture<String, String, String> replyFuture = kafkaTemplate.sendAndReceive(record);
    try {
      ConsumerRecord<String, String> consumerRecord = replyFuture.get(30, TimeUnit.SECONDS);
      if (consumerRecord == null) {
        throw new KafkaException("Got no reply");
      }
      final String stringReply = consumerRecord.value();

      return deserializeReply(stringReply, responseReader);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      // Handle exception
      throw new KafkaReplyTimeoutException("Timeout while waiting for reply");
    } catch (IOException e) {
      throw new RuntimeException("Wasn't able to deserialize the response!", e);
    }
  }

  private static <T> ResponseEntity<T> deserializeReply(String reply, ObjectReader responseReader) throws IOException {
    final JsonNode replyBody = MAPPER.readTree(reply);

    final T body = responseReader == null
        ? null
        : responseReader.readValue(replyBody.get(BODY_PARAM));
    final HttpHeaders headers = HEADERS_READER.readValue(replyBody.get(HEADERS_PARAM));
    final HttpStatus status = HttpStatus.valueOf(replyBody.get(STATUS_PARAM).asText());

    return new ResponseEntity<>(body, headers, status);
  }

  private static HttpHeaders toHttpHeaders(LinkedHashMap<String, String> linkedHashMap) {
    Map<String, List<String>> multiValueMap = linkedHashMap.entrySet().stream()
        .collect(Collectors.groupingBy(Entry::getKey,
            Collectors.collectingAndThen(Collectors.toList(),
                entryList -> entryList.stream()
                    .map(Entry::getValue)
                    .collect(Collectors.toList()))));

    final HttpHeaders headers = new HttpHeaders();
    headers.putAll(multiValueMap);

    return headers;
  }

}
