package com.cloudera.parserchains.queryservice.service;

import com.cloudera.parserchains.queryservice.common.exception.KafkaClusterNotFound;
import com.cloudera.parserchains.queryservice.common.exception.KafkaException;
import com.cloudera.parserchains.queryservice.config.kafka.ClouderaReplyingKafkaTemplate;
import com.cloudera.service.common.Utils;
import com.cloudera.service.common.request.RequestBody;
import com.cloudera.service.common.request.RequestType;
import com.cloudera.service.common.response.ClusterMeta;
import com.cloudera.service.common.response.ResponseBody;
import com.cloudera.service.common.response.ResponseType;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
public class KafkaService {
    private final Map<String, ClouderaReplyingKafkaTemplate<String, RequestBody, ResponseBody>> kafkaTemplatePool;
    private final Long replyFutureTimeout;
    private final Long kafkaTemplateTimeout;


    public KafkaService(@Qualifier("kafkaTemplatePool") Map<String, ClouderaReplyingKafkaTemplate<String, RequestBody, ResponseBody>> kafkaTemplatePool,
                        @Value("${kafka.reply.future.timeout:45}") Long replyFutureTimeout,
                        @Value("${kafka.reply.timeout:45}") Long kafkaTemplateTimeout) {
        this.kafkaTemplatePool = kafkaTemplatePool;
        this.replyFutureTimeout = replyFutureTimeout;
        this.kafkaTemplateTimeout = kafkaTemplateTimeout;
    }


    public Pair<ResponseType, ResponseBody> sendWithReply(RequestType requestType, String clusterId, RequestBody body) {
        final ClouderaReplyingKafkaTemplate<String, RequestBody, ResponseBody> kafkaTemplate = kafkaTemplatePool.get(clusterId);
        if (kafkaTemplate == null) {
            log.error("Cluster not found with cluster id: '{}'", clusterId);
            throw new KafkaClusterNotFound("Cluster not found! with cluster id '" + clusterId + "'");
        }
        return send(requestType, body, clusterId, kafkaTemplate);
    }

    public List<Pair<ResponseType, ResponseBody>> sendWithReply(RequestType requestType, RequestBody body) {
        return kafkaTemplatePool.entrySet().stream()
                .map(entry -> send(requestType, body, entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private Pair<ResponseType, ResponseBody> send(RequestType requestType, RequestBody body, String clusterId, ClouderaReplyingKafkaTemplate<String, RequestBody, ResponseBody> kafkaTemplate) {
        ProducerRecord<String, RequestBody> producerRecord = new ProducerRecord<>(kafkaTemplate.getRequestTopic(), requestType.name(), body);
        RequestReplyFuture<String, RequestBody, ResponseBody> replyFuture = kafkaTemplate.sendAndReceive(producerRecord, Duration.ofSeconds(kafkaTemplateTimeout));
        try {
            ConsumerRecord<String, ResponseBody> consumerRecord = replyFuture.get(replyFutureTimeout, TimeUnit.SECONDS);
            if (consumerRecord == null) {
                throw new KafkaException("Got no reply from kafka");
            }
            ResponseBody responseBody = consumerRecord.value();
            return Pair.of(Utils.getEnumFromString(consumerRecord.key(), ResponseType.class, ResponseType::name), responseBody);
        } catch (ExecutionException e) {
            // Handle exception
            log.error("Exception thrown when attempting to retrieve the information from kafka. Message: '{}' \n Cause: '{}'", e.getMessage(), e.getCause().getMessage());
            return Pair.of(null, ResponseBody.builder().clusterMeta(ClusterMeta.builder().clusterId(clusterId).clusterStatus("offline").build()).build());
        } catch (TimeoutException e) {
            // Handle exception
            log.error("Timeout while waiting for reply");
            return Pair.of(null, ResponseBody.builder().clusterMeta(ClusterMeta.builder().clusterId(clusterId).clusterStatus("offline").build()).build());
        } catch (InterruptedException e) {
            log.warn("Kafka throws interruption exception. Message: '{}' Cause: '{}'", e.getMessage(), Optional.ofNullable(e.getCause()).map(Throwable::getMessage).orElse("Cause is empty"));
            return Pair.of(null, ResponseBody.builder().clusterMeta(ClusterMeta.builder().clusterId(clusterId).clusterStatus("offline").build()).build());
        }
    }
}
