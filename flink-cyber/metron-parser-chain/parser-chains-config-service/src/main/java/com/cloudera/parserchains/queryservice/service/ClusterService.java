package com.cloudera.parserchains.queryservice.service;

import com.cloudera.parserchains.queryservice.common.exception.FailedAllClusterReponseException;
import com.cloudera.parserchains.queryservice.common.exception.FailedClusterReponseException;
import com.cloudera.service.common.request.RequestBody;
import com.cloudera.service.common.request.RequestType;
import com.cloudera.service.common.response.ResponseBody;
import com.cloudera.service.common.response.ResponseType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClusterService {
    private final KafkaService kafkaService;
    public List<ResponseBody> getAllClusterInfo() throws FailedAllClusterReponseException {
        List<Pair<ResponseType, ResponseBody>> response = kafkaService.sendWithReply(RequestType.GET_ALL_CLUSTERS_SERVICE_REQUEST, RequestBody.builder().build());
        List<ResponseBody> failedResponses = response.stream().filter(pair -> ResponseType.GET_ALL_CLUSTERS_SERVICE_RESPONSE != pair.getKey()).map(Pair::getValue).collect(Collectors.toList());
        if (!failedResponses.isEmpty()) {
            throw new FailedAllClusterReponseException(failedResponses);
        }
        return response.stream().map(Pair::getValue).collect(Collectors.toList());
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
}
