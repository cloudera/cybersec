package com.cloudera.parserchains.queryservice.service;


import com.cloudera.parserchains.queryservice.common.exception.FailedClusterReponseException;
import com.cloudera.parserchains.queryservice.common.exception.FailedJobAction;
import com.cloudera.parserchains.queryservice.common.exception.JobValidationException;
import com.cloudera.parserchains.queryservice.model.enums.JobActions;
import com.cloudera.service.common.Utils;
import com.cloudera.service.common.request.RequestBody;
import com.cloudera.service.common.request.RequestType;
import com.cloudera.service.common.response.ResponseBody;
import com.cloudera.service.common.response.ResponseType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {
    private final KafkaService kafkaService;
    private final ClusterService clusterService;

    public ResponseBody makeRequest(String clusterId, RequestBody body, String actionString) throws FailedClusterReponseException {
        JobActions action = Utils.getEnumFromString(actionString, JobActions.class, JobActions::getAction);
        switch (action) {
            case START:
                throw new UnsupportedOperationException("Start is not supported");
            case STOP:
                validateJobId(body, action.getAction());
                return sendAction(clusterId, body, RequestType.STOP_JOB_REQUEST, ResponseType.STOP_JOB_RESPONSE);
            case RESTART:
                validateJobId(body, action.getAction());
                return sendAction(clusterId, body, RequestType.RESTART_JOB_REQUEST, ResponseType.RESTART_JOB_RESPONSE);
            case UPDATE_CONFIG:
                validateJobId(body, action.getAction());
                return sendAction(clusterId, body, RequestType.UPDATE_JOB_CONFIG_REQUEST, ResponseType.UPDATE_JOB_CONFIG_RESPONSE);
            case GET_CONFIG:
                validateJobId(body, action.getAction());
                return sendAction(clusterId, body, RequestType.GET_JOB_CONFIG_REQUEST, ResponseType.GET_JOB_CONFIG_RESPONSE);
            case STATUS:
                validateJobId(body, action.getAction());
                return clusterService.getClusterInfo(clusterId, body);
            default:
                throw new FailedJobAction("Invalid action: " + actionString);
        }
    }

    private ResponseBody sendAction(String clusterId, RequestBody body, RequestType requestType, ResponseType responseType) {
        Pair<ResponseType, ResponseBody> response = kafkaService.sendWithReply(requestType, clusterId, body);
        if (response.getKey() != responseType) {
            throw new FailedJobAction("Failed to " + requestType.name() +" a job: " + response.getValue());
        }
        return response.getValue();
    }

    private void validateJobId(RequestBody body, String action) {
        if (StringUtils.isEmpty(body.getJobIdHex())) {
            throw new JobValidationException("Job ID is required to " + action + " a job");
        }
    }
}
