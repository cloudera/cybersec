package com.cloudera.parserchains.queryservice.service;

import com.cloudera.parserchains.queryservice.common.exception.FailedAllClusterReponseException;
import com.cloudera.parserchains.queryservice.common.exception.FailedClusterReponseException;
import com.cloudera.parserchains.queryservice.common.utils.Utils;
import com.cloudera.service.common.request.RequestBody;
import com.cloudera.service.common.request.RequestType;
import com.cloudera.service.common.response.Job;
import com.cloudera.service.common.response.Pipeline;
import com.cloudera.service.common.response.ResponseBody;
import com.cloudera.service.common.response.ResponseType;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClusterService {
    private final KafkaServiceInterface kafkaService;

    public List<ResponseBody> getAllClusterInfo() throws FailedAllClusterReponseException {
        List<Pair<ResponseType, ResponseBody>> response = kafkaService.sendWithReply(RequestType.GET_ALL_CLUSTERS_SERVICE_REQUEST, RequestBody.builder().build());
        List<ResponseBody> failedResponses = response.stream()
                .filter(pair -> pair.getKey() != null && ResponseType.GET_ALL_CLUSTERS_SERVICE_RESPONSE != pair.getKey())
                .map(Pair::getValue)
                .collect(Collectors.toList());
        if (!failedResponses.isEmpty()) {
            throw new FailedAllClusterReponseException(failedResponses);
        }
        return response.stream().map(Pair::getValue).collect(Collectors.toList());
    }

    public List<Pipeline> getAllPipelines() throws FailedAllClusterReponseException {
        return getAllClusterInfo().stream()
                .flatMap(responseBody -> getClusterPipelines(responseBody).stream())
                .collect(Collectors.toList());
    }

    public List<Pipeline> getClusterPipelines(ResponseBody responseBody) {
        ArrayList<Pipeline> result = new ArrayList<>();
        for (Job job : responseBody.getJobs()) {
            Optional<Pipeline> duplicatePipeline = result.stream().filter(pipeline -> StringUtils.equalsIgnoreCase(pipeline.getName(), job.getJobPipeline())).findFirst();
            if (duplicatePipeline.isPresent()) {
                Pipeline pipeline = duplicatePipeline.get();
                List<String> pipelineJobs = pipeline.getJobs();
                if (!pipelineJobs.contains(job.getJobType().getName())) {
                    pipelineJobs.add(job.getJobType().getName());
                }
                if (isNewJobTimeLater(job, pipeline)) {
                    pipeline.setDate(job.getStartTime());
                }
            } else {
                result.add(Pipeline.builder()
                        .name(job.getJobPipeline())
                        .clusterName(responseBody.getClusterMeta().getName())
                        .date(job.getStartTime())
                        .jobs(Lists.newArrayList(job.getJobType().getName()))
                        .userName(job.getUser())
                        .build());
            }
        }
        return result;
    }

    public ResponseBody createEmptyPipeline(String clusterId, RequestBody body) throws FailedClusterReponseException {
        RequestBody requestBody = RequestBody.builder()
                .pipelineName(body.getPipelineName())
                .branch(body.getBranch())
                .build();
        Pair<ResponseType, ResponseBody> response = kafkaService.sendWithReply(RequestType.CREATE_EMPTY_PIPELINE, clusterId, requestBody);
        if (response.getKey() != ResponseType.CREATE_EMPTY_PIPELINE_RESPONSE) {
            throw new FailedClusterReponseException(response.getValue());
        }
        return response.getValue();
    }

    public ResponseBody startPipelineJob(String clusterId, String pipeline, String branch, String profileName, String parserName, List<String> jobs, byte[] payload) throws FailedClusterReponseException {
        Pair<ResponseType, ResponseBody> response = kafkaService.sendWithReply(RequestType.START_PIPELINE, clusterId, RequestBody
                .builder()
                .payload(Base64.getEncoder().encode(payload))
                .branch(branch)
                .profileName(profileName)
                .parserName(parserName)
                .jobs(jobs)
                .pipelineName(pipeline)
                .build());
        if (response.getKey() != ResponseType.START_PIPELINE_RESPONSE) {
            throw new FailedClusterReponseException(response.getValue());
        }
        return response.getValue();
    }


    public ResponseBody getClusterInfo(String clusterId) throws FailedClusterReponseException {
        return getClusterInfo(clusterId, RequestBody.builder().build());
    }

    public ResponseBody getClusterInfo(String clusterId, RequestBody body) throws FailedClusterReponseException {
        Pair<ResponseType, ResponseBody> response = kafkaService.sendWithReply(RequestType.GET_CLUSTER_SERVICE_REQUEST, clusterId, body);
        if (response.getKey() != ResponseType.GET_CLUSTER_SERVICE_RESPONSE) {
            throw new FailedClusterReponseException(response.getValue());
        }
        return response.getValue();
    }

    private static boolean isNewJobTimeLater(Job job, Pipeline pipeline) {
        return Utils.compareLongs(Utils.parseData(job.getStartTime(), Utils.DATE_FORMATS),
                (Utils.parseData(pipeline.getDate(), Utils.DATE_FORMATS))) == 1;
    }
}
