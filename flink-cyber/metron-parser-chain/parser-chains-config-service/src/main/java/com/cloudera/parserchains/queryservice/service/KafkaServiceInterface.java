package com.cloudera.parserchains.queryservice.service;

import com.cloudera.service.common.request.RequestBody;
import com.cloudera.service.common.request.RequestType;
import com.cloudera.service.common.response.ResponseBody;
import com.cloudera.service.common.response.ResponseType;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public interface KafkaServiceInterface {
    Pair<ResponseType, ResponseBody> sendWithReply(RequestType requestType, String clusterId, RequestBody body);

    List<Pair<ResponseType, ResponseBody>> sendWithReply(RequestType requestType, RequestBody body);
}
