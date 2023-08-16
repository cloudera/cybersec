package com.cloudera.cyber.restcli.controller;

import com.cloudera.cyber.restcli.service.JobService;
import com.cloudera.service.common.Utils;
import com.cloudera.service.common.request.RequestBody;
import com.cloudera.service.common.request.RequestType;
import com.cloudera.service.common.response.Job;
import com.cloudera.service.common.response.ResponseBody;
import com.cloudera.service.common.response.ResponseType;
import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
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
public class KafkaListenerController {

    private final JobService jobService;

    @KafkaListener(topics = "#{kafkaProperties.getRequestTopic()}", containerFactory = "kafkaListenerContainerFactory")
    @SendTo({"#{kafkaProperties.getReplyTopic()}"})
    public Message<ResponseBody> replyTest(RequestBody requestBody, @Header(KafkaHeaders.MESSAGE_KEY) String key) {
        RequestType requestType = Utils.getEnumFromString(key, RequestType.class, RequestType::name);
        switch (requestType) {
            case GET_ALL_CLUSTERS_SERVICE_REQUEST:
                try {
                    List<Job> jobs = jobService.getJobs();
                    ResponseBody responseBody = ResponseBody.builder()
                            .jobs(jobs)
                            .build();
                    return buildResponseMessage(responseBody, ResponseType.GET_ALL_CLUSTERS_SERVICE_RESPONSE);
                } catch (IOException | InterruptedException e) {
                    return handleErrorResponse(e);
                }
            case GET_CLUSTER_SERVICE_REQUEST:
                try {
                    Job job = jobService.getJob(requestBody.getJobIdHex());
                    ResponseBody responseBody = ResponseBody.builder()
                            .jobs(Collections.singletonList(job))
                            .build();
                    return buildResponseMessage(responseBody, ResponseType.GET_ALL_CLUSTERS_SERVICE_RESPONSE);
                } catch (IOException | InterruptedException e) {
                    return handleErrorResponse(e);
                }
            case START_JOB_REQUEST:
                break;
            case RESTART_JOB_REQUEST:
                try {
                    Job job = jobService.restartJob(requestBody.getJobIdHex(), requestBody.getPipelineDir());
                    ResponseBody responseBody = ResponseBody.builder()
                            .jobs(Collections.singletonList(job))
                            .build();
                    return buildResponseMessage(responseBody, ResponseType.GET_ALL_CLUSTERS_SERVICE_RESPONSE);
                } catch (IOException | InterruptedException e) {
                    return handleErrorResponse(e);
                }
            case STOP_JOB_REQUEST:
                try {
                    Job job = jobService.stopJob(requestBody.getJobIdHex());
                    ResponseBody responseBody = ResponseBody.builder()
                            .jobs(Collections.singletonList(job))
                            .build();
                    return buildResponseMessage(responseBody, ResponseType.GET_ALL_CLUSTERS_SERVICE_RESPONSE);
                } catch (IOException | InterruptedException e) {
                    return handleErrorResponse(e);
                }
            case GET_JOB_CONFIG_REQUEST:
                break;
            case UPDATE_JOB_CONFIG_REQUEST:
                break;
        }
        return null;
    }

    private Message<ResponseBody> handleErrorResponse(Exception e) {
        ResponseBody responseBody = ResponseBody.builder()
                .errorMessage(Collections.singletonMap(e.getClass().toString(), e.getMessage()))
                .build();
        Thread.currentThread().interrupt();
        return buildResponseMessage(responseBody, ResponseType.ERROR_RESPONSE);
    }

    private Message<ResponseBody> buildResponseMessage(ResponseBody body, ResponseType responseType) {
        MessageHeaderAccessor accessor = new MessageHeaderAccessor();
        accessor.setHeader(KafkaHeaders.MESSAGE_KEY, responseType.name());
        MessageHeaders headers = accessor.getMessageHeaders();
        return MessageBuilder.createMessage(body, headers);
    }
}
