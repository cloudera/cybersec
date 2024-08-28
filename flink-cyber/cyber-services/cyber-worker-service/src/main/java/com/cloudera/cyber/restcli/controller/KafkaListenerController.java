package com.cloudera.cyber.restcli.controller;

import com.cloudera.cyber.restcli.configuration.AppWorkerConfig;
import com.cloudera.cyber.restcli.service.JobService;
import com.cloudera.cyber.restcli.service.FilePipelineService;
import com.cloudera.service.common.Utils;
import com.cloudera.service.common.request.RequestBody;
import com.cloudera.service.common.request.RequestType;
import com.cloudera.service.common.response.ClusterMeta;
import com.cloudera.service.common.response.Job;
import com.cloudera.service.common.response.ResponseBody;
import com.cloudera.service.common.response.ResponseType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaListenerController {
    private final JobService jobService;
    private final FilePipelineService pipelineService;
    private final AppWorkerConfig config;

    //TODO:  Rewrite to Spring events. Probably split the events into separate types, such as cluster event, job event, pipeline event, etc.
    @KafkaListener(topics = "#{kafkaProperties.getRequestTopic()}", containerFactory = "kafkaListenerContainerFactory")
    @SendTo({"#{kafkaProperties.getReplyTopic()}"})
    public Message<ResponseBody> handleMessage(RequestBody requestBody, @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key, @Header(KafkaHeaders.REPLY_TOPIC) byte[] replyTo,
                                               @Header(KafkaHeaders.CORRELATION_ID) byte[] correlationId) {
        log.info("Start processing message\n Message key: '{}' \n value: '{}'", key, requestBody);

        RequestType requestType = Utils.getEnumFromString(key, RequestType.class, RequestType::name);
        switch (requestType) {
            case GET_ALL_CLUSTERS_SERVICE_REQUEST:
                return getResponseBodyMessage(replyTo, correlationId, ResponseType.GET_ALL_CLUSTERS_SERVICE_RESPONSE);
            case GET_CLUSTER_SERVICE_REQUEST:
                return getResponseBodyMessage(replyTo, correlationId, ResponseType.GET_CLUSTER_SERVICE_RESPONSE);
            case START_JOB_REQUEST:
                break;
            case RESTART_JOB_REQUEST:
                try {
                    Job job = jobService.restartJob(requestBody.getJobIdHex());
                    ResponseBody responseBody = ResponseBody.builder()
                            .jobs(Collections.singletonList(job))
                            .build();
                    return buildResponseMessage(responseBody, ResponseType.RESTART_JOB_RESPONSE, replyTo, correlationId);
                } catch (IOException e) {
                    return handleErrorResponse(e, replyTo, correlationId);
                }
            case STOP_JOB_REQUEST:
                try {
                    Job job = jobService.stopJob(requestBody.getJobIdHex());
                    ResponseBody responseBody = ResponseBody.builder()
                            .jobs(Collections.singletonList(job))
                            .build();
                    return buildResponseMessage(responseBody, ResponseType.STOP_JOB_RESPONSE, replyTo, correlationId);
                } catch (IOException e) {
                    return handleErrorResponse(e, replyTo, correlationId);
                }
            case GET_JOB_CONFIG_REQUEST:
                break;
            case UPDATE_JOB_CONFIG_REQUEST:
                try {
                    jobService.updateConfig(requestBody.getPayload());
                    final ResponseBody responseBody = ResponseBody.builder().build();
                    return buildResponseMessage(responseBody, ResponseType.UPDATE_JOB_CONFIG_RESPONSE, replyTo, correlationId);
                } catch (IOException e) {
                    return handleErrorResponse(e, replyTo, correlationId);
                }
            case CREATE_EMPTY_PIPELINE:
                try {
                    pipelineService.createEmptyPipeline(requestBody.getPipelineName(), requestBody.getBranch());
                    final ResponseBody responseBody = ResponseBody.builder().build();
                    return buildResponseMessage(responseBody, ResponseType.CREATE_EMPTY_PIPELINE_RESPONSE, replyTo, correlationId);
                } catch (Exception e) {
                    return handleErrorResponse(e, replyTo, correlationId);
                }
            case START_PIPELINE:
                try {
                    pipelineService.extractPipeline(requestBody.getPayload(), requestBody.getPipelineName(), requestBody.getBranch());
                    pipelineService.startPipelineJob(requestBody.getPipelineName(), requestBody.getBranch(), requestBody.getProfileName(), requestBody.getProfileName(), requestBody.getJobs());
                    final ResponseBody responseBody = ResponseBody.builder().build();
                    return buildResponseMessage(responseBody, ResponseType.START_PIPELINE_RESPONSE, replyTo, correlationId);
                } catch (Exception e) {
                    log.error("Exception while processing the Start All request {}", e.getMessage());
                    return handleErrorResponse(e, replyTo, correlationId);

                }

        }
        return null;
    }

    private Message<ResponseBody> getResponseBodyMessage(byte[] replyTo, byte[] correlationId, ResponseType responseType) {
        try {
            List<Job> jobs = jobService.getJobs();
            ResponseBody responseBody = ResponseBody.builder()
                    .jobs(jobs)
                    .clusterMeta(ClusterMeta.builder()
                            .name(config.getName())
                            .clusterId(config.getId())
                            .clusterStatus(config.getStatus())
                            .version(config.getVersion())
                            .build())
                    .build();
            return buildResponseMessage(responseBody, responseType, replyTo, correlationId);
        } catch (IOException e) {
            return handleErrorResponse(e, replyTo, correlationId);
        }
    }

    private Message<ResponseBody> handleErrorResponse(Exception e, byte[] replyTo, byte[] correlationId) {
        ResponseBody responseBody = ResponseBody.builder()
                .errorMessage(Collections.singletonMap(e.getClass().toString(), e.getMessage()))
                .build();
        return buildResponseMessage(responseBody, ResponseType.ERROR_RESPONSE, replyTo, correlationId);
    }

    private Message<ResponseBody> buildResponseMessage(ResponseBody body, ResponseType responseType, byte[] replyTo, byte[] correlationId) {
        MessageHeaderAccessor accessor = new MessageHeaderAccessor();
        accessor.setHeader(KafkaHeaders.MESSAGE_KEY, responseType.name());
        accessor.setHeader(KafkaHeaders.CORRELATION_ID, correlationId);
        MessageHeaders headers = accessor.getMessageHeaders();
        return MessageBuilder.createMessage(body, headers);
    }
}
