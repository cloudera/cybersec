package com.cloudera.cyber.restcli.controller;

import com.cloudera.cyber.restcli.service.JobService;
import com.cloudera.service.common.Utils;
import com.cloudera.service.common.request.RequestBody;
import com.cloudera.service.common.request.RequestType;
import com.cloudera.service.common.response.Job;
import com.cloudera.service.common.response.ResponseBody;
import com.cloudera.service.common.response.ResponseType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.kafka.support.KafkaHeaders;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaListenerController {

    private final JobService jobService;

    @KafkaListener(topics = "#{kafkaProperties.getRequestTopic()}", containerFactory = "kafkaListenerContainerFactory")
    @SendTo({"#{kafkaProperties.getReplyTopic()}"})
    public Message<ResponseBody> replyTest(RequestBody requestBody, @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key, @Header(KafkaHeaders.REPLY_TOPIC) byte[] replyTo,
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
                } catch (IOException | InterruptedException e) {
                    return handleErrorResponse(e, replyTo, correlationId);
                }
            case STOP_JOB_REQUEST:
                try {
                    Job job = jobService.stopJob(requestBody.getJobIdHex());
                    ResponseBody responseBody = ResponseBody.builder()
                            .jobs(Collections.singletonList(job))
                            .build();
                    return buildResponseMessage(responseBody, ResponseType.STOP_JOB_RESPONSE, replyTo, correlationId);
                } catch (IOException | InterruptedException e) {
                    return handleErrorResponse(e, replyTo, correlationId);
                }
            case GET_JOB_CONFIG_REQUEST:
                break;
            case UPDATE_JOB_CONFIG_REQUEST:
                break;
        }
        return null;
    }

    private Message<ResponseBody> getResponseBodyMessage(byte[] replyTo, byte[] correlationId, ResponseType responseType) {
        try {
            List<Job> jobs = jobService.getJobs();
            ResponseBody responseBody = ResponseBody.builder()
                    .jobs(jobs)
                    .build();
            return buildResponseMessage(responseBody, responseType, replyTo, correlationId);
        } catch (IOException | InterruptedException e) {
            return handleErrorResponse(e, replyTo, correlationId);
        }
    }

    private Message<ResponseBody> handleErrorResponse(Exception e, byte[] replyTo, byte[] correlationId) {
        ResponseBody responseBody = ResponseBody.builder()
                .errorMessage(Collections.singletonMap(e.getClass().toString(), e.getMessage()))
                .build();
        Thread.currentThread().interrupt();
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
