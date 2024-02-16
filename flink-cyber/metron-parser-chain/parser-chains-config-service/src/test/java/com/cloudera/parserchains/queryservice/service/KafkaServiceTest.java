package com.cloudera.parserchains.queryservice.service;

import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.BODY_PARAM;
import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.HEADERS_PARAM;
import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.STATUS_PARAM;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import com.cloudera.parserchains.core.utils.JSONUtils;
import com.cloudera.parserchains.queryservice.common.ApplicationConstants;
import com.cloudera.parserchains.queryservice.config.kafka.ClouderaReplyingKafkaTemplate;
import com.cloudera.parserchains.queryservice.model.enums.KafkaMessageType;
import com.cloudera.parserchains.queryservice.model.summary.ParserChainSummary;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.requestreply.KafkaReplyTimeoutException;
import org.springframework.kafka.requestreply.RequestReplyFuture;

public class KafkaServiceTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final ObjectReader OBJECT_READER = OBJECT_MAPPER.readerFor(OBJECT_MAPPER.getTypeFactory()
        .constructCollectionType(List.class, ParserChainSummary.class));

  private static final KafkaMessageType MESSAGE_TYPE = KafkaMessageType.CHAIN_FIND_ALL;
  private static final String REQUEST_TOPIC = "requestTopic";
  private static final String CLUSTER_ID = "clusterId";

  private ClouderaReplyingKafkaTemplate<String, String, String> kafkaTemplate;

  private KafkaService kafkaService;

  @BeforeEach
  void beforeEach() {
    kafkaTemplate = Mockito.mock(ClouderaReplyingKafkaTemplate.class);

    Map<String, ClouderaReplyingKafkaTemplate<String, String, String>> kafkaTemplatePool = new HashMap<>();
    kafkaTemplatePool.put(CLUSTER_ID, kafkaTemplate);

    kafkaService = new KafkaService(kafkaTemplatePool);
  }

  @Test
  @SneakyThrows
  public void shouldSendWithReply() {
    //Given
    final ResponseEntity<List<ParserChainSummary>> expectedResult = getDefaultResponseEntity();

    final String jsonReplyBody = getJsonBody(expectedResult);

    final Map<String, Object> requestBody = new HashMap<>();
    requestBody.put(ApplicationConstants.PIPELINE_NAME_PARAM, "pipelineName");

    final ProducerRecord<String, String> givenProducerRecord = new ProducerRecord<>(REQUEST_TOPIC,
        MESSAGE_TYPE.name(), JSONUtils.INSTANCE.toJSON(requestBody, false));
    final ConsumerRecord<String, String> givenConsumerRecord = new ConsumerRecord<>(REQUEST_TOPIC, 1, 1,
        MESSAGE_TYPE.name(), jsonReplyBody);

    RequestReplyFuture<String, String, String> replyFuture = new RequestReplyFuture<>();
    replyFuture.set(givenConsumerRecord);

    //When
    Mockito.when(kafkaTemplate.getRequestTopic()).thenReturn(REQUEST_TOPIC);
    Mockito.when(kafkaTemplate.sendAndReceive(any(ProducerRecord.class))).thenAnswer(invocation -> {
      final ProducerRecord<String, String> producerRecord = invocation.getArgument(0);

      assertThat(producerRecord.topic(), equalTo(givenProducerRecord.topic()));
      assertThat(producerRecord.key(), equalTo(givenProducerRecord.key()));
      assertThat(producerRecord.value(), equalTo(givenProducerRecord.value()));

      return replyFuture;
    });

    final ResponseEntity<Object> responseEntity = kafkaService.sendWithReply(MESSAGE_TYPE, CLUSTER_ID,
        requestBody, OBJECT_READER);

    //Then
    assertThat(responseEntity, equalTo(expectedResult));
    verify(kafkaTemplate, times(1)).getRequestTopic();
    verify(kafkaTemplate, times(1)).sendAndReceive(any(ProducerRecord.class));
  }

  @Test
  public void shouldThrowWhenClusterNotFound() {
    try {
      kafkaService.sendWithReply(MESSAGE_TYPE, "invalidClusterId",
          null, OBJECT_READER);
    } catch (Exception e) {
      assertThat(e.getClass(), equalTo(RuntimeException.class));
      assertThat(e.getMessage(), equalTo("Cluster not found!"));
    }
    verify(kafkaTemplate, times(0)).getRequestTopic();
    verify(kafkaTemplate, times(0)).sendAndReceive(any(ProducerRecord.class));
  }

  @Test
  public void shouldThrowWhenNoKafkaReply() {
    Mockito.when(kafkaTemplate.getRequestTopic()).thenReturn(REQUEST_TOPIC);
    final RequestReplyFuture<String, String, String> replyFuture = new RequestReplyFuture<>();
    replyFuture.set(null);
    Mockito.when(kafkaTemplate.sendAndReceive(any(ProducerRecord.class)))
        .thenReturn(replyFuture);

    try {
      kafkaService.sendWithReply(MESSAGE_TYPE, CLUSTER_ID,
          null, OBJECT_READER);
    } catch (Exception e) {
      assertThat(e.getClass(), equalTo(KafkaException.class));
      assertThat(e.getMessage(), equalTo("Got no reply"));
    }
    verify(kafkaTemplate, times(1)).getRequestTopic();
    verify(kafkaTemplate, times(1)).sendAndReceive(any(ProducerRecord.class));
  }

  @Test
  public void shouldThrowWhenKafkaReplyTimeOut() {
    Mockito.when(kafkaTemplate.getRequestTopic()).thenReturn(REQUEST_TOPIC);
    final RequestReplyFuture<String, String, String> replyFuture = new RequestReplyFuture<>();
    Mockito.when(kafkaTemplate.sendAndReceive(any(ProducerRecord.class)))
        .thenReturn(replyFuture);

    try {
      kafkaService.sendWithReply(MESSAGE_TYPE, CLUSTER_ID,
          null, OBJECT_READER);
    } catch (Exception e) {
      assertThat(e.getClass(), equalTo(KafkaReplyTimeoutException.class));
      assertThat(e.getMessage(), equalTo("Timeout while waiting for reply"));
    }
    verify(kafkaTemplate, times(1)).getRequestTopic();
    verify(kafkaTemplate, times(1)).sendAndReceive(any(ProducerRecord.class));
  }

  @Test
  public void shouldThrowWhenCantDeserialize() {
    Mockito.when(kafkaTemplate.getRequestTopic()).thenReturn(REQUEST_TOPIC);
    final ConsumerRecord<String, String> invalidRecord = new ConsumerRecord<>(REQUEST_TOPIC, 1, 1, MESSAGE_TYPE.name(),
        "invalidValue");
    final RequestReplyFuture<String, String, String> replyFuture = new RequestReplyFuture<>();
    replyFuture.set(invalidRecord);
    Mockito.when(kafkaTemplate.sendAndReceive(any(ProducerRecord.class)))
        .thenReturn(replyFuture);

    try {
      kafkaService.sendWithReply(MESSAGE_TYPE, CLUSTER_ID,
          null, OBJECT_READER);
    } catch (Exception e) {
      assertThat(e.getClass(), equalTo(RuntimeException.class));
      assertThat(e.getMessage(), equalTo("Wasn't able to deserialize the response!"));
    }
    verify(kafkaTemplate, times(1)).getRequestTopic();
    verify(kafkaTemplate, times(1)).sendAndReceive(any(ProducerRecord.class));
  }

  private static String getJsonBody(ResponseEntity<List<ParserChainSummary>> response) throws JsonProcessingException {
    final Map<String, Object> givenReply = new HashMap<>();
    givenReply.put(BODY_PARAM, response.getBody());
    givenReply.put(HEADERS_PARAM, response.getHeaders());
    givenReply.put(STATUS_PARAM, response.getStatusCode());
    return new ObjectMapper().writeValueAsString(givenReply);
  }

  private static ResponseEntity<List<ParserChainSummary>> getDefaultResponseEntity() throws JsonProcessingException {
    final ParserChainSummary givenEntity = new ParserChainSummary();
    givenEntity.setId("testId");
    givenEntity.setName("testName");
    final List<ParserChainSummary> replyBody = Collections.singletonList(givenEntity);

    final HttpHeaders givenHeaders = new HttpHeaders();
    givenHeaders.set("testHeader", "testValue");

    return new ResponseEntity<>(replyBody, givenHeaders, HttpStatus.OK);
  }

}