package com.cloudera.parserchains.queryservice.service;

import com.cloudera.parserchains.queryservice.common.exception.KafkaClusterNotFound;
import com.cloudera.parserchains.queryservice.common.exception.KafkaException;
import com.cloudera.parserchains.queryservice.config.kafka.ClouderaReplyingKafkaTemplate;
import com.cloudera.service.common.Utils;
import com.cloudera.service.common.request.RequestBody;
import com.cloudera.service.common.request.RequestType;
import com.cloudera.service.common.response.ResponseBody;
import com.cloudera.service.common.response.ResponseType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaService {
    @Qualifier("kafkaTemplatePool")
    private final Map<String, ClouderaReplyingKafkaTemplate<String, RequestBody, ResponseBody>> kafkaTemplatePool;

    public Pair<ResponseType, ResponseBody> sendWithReply(RequestType requestType, String clusterId, RequestBody body) {
        final ClouderaReplyingKafkaTemplate<String, RequestBody, ResponseBody> kafkaTemplate = kafkaTemplatePool.get(clusterId);
        if (kafkaTemplate == null) {
            log.error("Cluster not found with cluster id: '{}'", clusterId);
            throw new KafkaClusterNotFound("Cluster not found! with cluster id '" + clusterId + "'");
        }
        return send(requestType, body, kafkaTemplate);
    }

    public List<Pair<ResponseType, ResponseBody>> sendWithReply(RequestType requestType, RequestBody body) {
        return kafkaTemplatePool.values().stream().map(template -> send(requestType, body, template)).collect(Collectors.toList());
    }

    private Pair<ResponseType, ResponseBody> send(RequestType requestType, RequestBody body, ClouderaReplyingKafkaTemplate<String, RequestBody, ResponseBody> kafkaTemplate) {
        ProducerRecord<String, RequestBody> producerRecord = new ProducerRecord<>(kafkaTemplate.getRequestTopic(),
                requestType.name(), body);
        RequestReplyFuture<String, RequestBody, ResponseBody> replyFuture = kafkaTemplate.sendAndReceive(producerRecord, Duration.ofMinutes(2));
        try {
            ConsumerRecord<String, ResponseBody> consumerRecord = replyFuture.get(120, TimeUnit.SECONDS);
            if (consumerRecord == null) {
                throw new KafkaException("Got no reply from kafka");
            }
            return Pair.of(Utils.getEnumFromString(consumerRecord.key(), ResponseType.class, ResponseType::name), consumerRecord.value());
        } catch (ExecutionException e) {
            // Handle exception
            log.error("Exception thrown when attempting to retrieve the information from kafka. Message: '{}' \n Cause: '{}'", e.getMessage(), e.getCause().getMessage());
            throw new KafkaException("Exception thrown when attempting to retrieve the information from kafka.");
        } catch (TimeoutException e) {
            // Handle exception
            log.error("Timeout while waiting for reply");
            throw new KafkaException("Timeout while waiting for reply");
        } catch (InterruptedException e) {
            log.warn("Kafka throws interruption exception. Message: '{}' Cause: '{}'", e.getMessage(),
                    Optional.ofNullable(e.getCause()).map(Throwable::getMessage).orElse("Cause is empty"));
            Thread.currentThread().interrupt();
        }
        return Pair.of(null, null);
    }
}
