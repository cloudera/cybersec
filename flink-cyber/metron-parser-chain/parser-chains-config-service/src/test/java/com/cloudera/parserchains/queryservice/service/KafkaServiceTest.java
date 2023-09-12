package com.cloudera.parserchains.queryservice.service;

import com.cloudera.parserchains.queryservice.common.exception.KafkaClusterNotFound;
import com.cloudera.parserchains.queryservice.common.exception.KafkaException;
import com.cloudera.parserchains.queryservice.config.kafka.ClouderaReplyingKafkaTemplate;
import com.cloudera.service.common.request.RequestBody;
import com.cloudera.service.common.request.RequestType;
import com.cloudera.service.common.response.ResponseBody;
import com.cloudera.service.common.response.ResponseType;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.requestreply.RequestReplyFuture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaServiceTest {

    @InjectMocks
    private KafkaService kafkaService;

    @Mock
    private Map<String, ClouderaReplyingKafkaTemplate<String, RequestBody, ResponseBody>> kafkaTemplatePool;

    @Mock
    private ClouderaReplyingKafkaTemplate<String, RequestBody, ResponseBody> kafkaTemplate;

    @Mock
    private RequestReplyFuture<String, RequestBody, ResponseBody> replyFuture;

    @Mock
    private ConsumerRecord<String, ResponseBody> consumerRecord;


    @Test
    void testSendsWithReply() throws Exception {
        ResponseBody responseBody = ResponseBody.builder().build();

        when(kafkaTemplatePool.values()).thenReturn(Collections.singletonList(kafkaTemplate));
        when(kafkaTemplate.getRequestTopic()).thenReturn("testTopic");
        when(kafkaTemplate.sendAndReceive(ArgumentMatchers.<ProducerRecord<String, RequestBody>>any())).thenReturn(replyFuture);
        when(replyFuture.get(anyLong(), any())).thenReturn(consumerRecord);
        when(consumerRecord.key()).thenReturn(ResponseType.GET_ALL_CLUSTERS_SERVICE_RESPONSE.name());
        when(consumerRecord.value()).thenReturn(responseBody);

        List<Pair<ResponseType, ResponseBody>> result = kafkaService.sendWithReply(RequestType.GET_ALL_CLUSTERS_SERVICE_REQUEST, mock(RequestBody.class));

        List<String> list = new ArrayList<>();

        assertThat(result).isNotNull().hasSize(1).containsExactly(Pair.of(ResponseType.GET_ALL_CLUSTERS_SERVICE_RESPONSE, responseBody));
    }


    @Test
    void testSendWithReply() throws Exception {
        ResponseBody responseBody = ResponseBody.builder().build();

        when(kafkaTemplatePool.get(anyString())).thenReturn(kafkaTemplate);
        when(kafkaTemplate.getRequestTopic()).thenReturn("testTopic");
        when(kafkaTemplate.sendAndReceive(ArgumentMatchers.<ProducerRecord<String, RequestBody>>any())).thenReturn(replyFuture);
        when(replyFuture.get(anyLong(), any())).thenReturn(consumerRecord);
        when(consumerRecord.key()).thenReturn(ResponseType.GET_ALL_CLUSTERS_SERVICE_RESPONSE.name());
        when(consumerRecord.value()).thenReturn(responseBody);

        Pair<ResponseType, ResponseBody> result = kafkaService.sendWithReply(RequestType.GET_ALL_CLUSTERS_SERVICE_REQUEST, "testClusterId", mock(RequestBody.class));

        assertThat(result).isNotNull().extracting(Pair::getLeft, Pair::getRight).containsExactly(ResponseType.GET_ALL_CLUSTERS_SERVICE_RESPONSE, responseBody);

    }

    @Test
    void testSendWithReplyClusterNotFound() {
        when(kafkaTemplatePool.get(anyString())).thenReturn(null);
        String clusterId = "clusterId";


        KafkaClusterNotFound exception = assertThrows(KafkaClusterNotFound.class, () -> {
            kafkaService.sendWithReply(RequestType.GET_ALL_CLUSTERS_SERVICE_REQUEST, clusterId, mock(RequestBody.class));
        });

        assertThat(exception).isNotNull().hasMessageContaining(clusterId);
    }

    // add send with reply test that throws KafkaException
    @Test
    void testSendWithReplyKafkaException() throws Exception {
        when(kafkaTemplatePool.get(anyString())).thenReturn(kafkaTemplate);
        when(kafkaTemplate.getRequestTopic()).thenReturn("testTopic");
        when(kafkaTemplate.sendAndReceive(ArgumentMatchers.<ProducerRecord<String, RequestBody>>any())).thenReturn(replyFuture);
        when(replyFuture.get(anyLong(), any())).thenThrow(new TimeoutException("test exception"));

        KafkaException exception = assertThrows(KafkaException.class, () -> {
            kafkaService.sendWithReply(RequestType.GET_ALL_CLUSTERS_SERVICE_REQUEST, "testClusterId", mock(RequestBody.class));
        });

        assertThat(exception).isNotNull().hasMessageContaining("Timeout");
    }

    // add send with reply test that throws InterruptedException
    @Test
    void testSendWithReplyInterruptedException() throws Exception {
        when(kafkaTemplatePool.get(anyString())).thenReturn(kafkaTemplate);
        when(kafkaTemplate.getRequestTopic()).thenReturn("testTopic");
        when(kafkaTemplate.sendAndReceive(ArgumentMatchers.<ProducerRecord<String, RequestBody>>any())).thenReturn(replyFuture);
        when(replyFuture.get(anyLong(), any())).thenThrow(new InterruptedException("test exception"));

        Pair<ResponseType, ResponseBody> result = kafkaService.sendWithReply(RequestType.GET_ALL_CLUSTERS_SERVICE_REQUEST, "testClusterId", mock(RequestBody.class));

        assertThat(result).isNotNull().extracting(Pair::getLeft, Pair::getRight).containsExactly(null, null);
    }

    @Test
    void testSendWithReplyExecutionException() throws Exception {
        when(kafkaTemplatePool.get(anyString())).thenReturn(kafkaTemplate);
        when(kafkaTemplate.getRequestTopic()).thenReturn("testTopic");
        when(kafkaTemplate.sendAndReceive(ArgumentMatchers.<ProducerRecord<String, RequestBody>>any())).thenReturn(replyFuture);
        when(replyFuture.get(anyLong(), any())).thenThrow(new java.util.concurrent.ExecutionException("test exception", new RuntimeException("test exception")));

        KafkaException exception = assertThrows(KafkaException.class, () -> {
            kafkaService.sendWithReply(RequestType.GET_ALL_CLUSTERS_SERVICE_REQUEST, "testClusterId", mock(RequestBody.class));
        });

        assertThat(exception).isNotNull().hasMessageContaining("Exception thrown when attempting to retrieve the information from kafka");
    }

    @Test
    void testSendWithReplyNullConsumerRecord() throws Exception {
        when(kafkaTemplatePool.get(anyString())).thenReturn(kafkaTemplate);
        when(kafkaTemplate.getRequestTopic()).thenReturn("testTopic");
        when(kafkaTemplate.sendAndReceive(ArgumentMatchers.<ProducerRecord<String, RequestBody>>any())).thenReturn(replyFuture);
        when(replyFuture.get(anyLong(), any())).thenReturn(null);

        KafkaException exception = assertThrows(KafkaException.class, () -> {
            kafkaService.sendWithReply(RequestType.GET_ALL_CLUSTERS_SERVICE_REQUEST, "testClusterId", mock(RequestBody.class));
        });

        assertThat(exception).isNotNull().hasMessageContaining("Got no reply from kafka");
    }
}