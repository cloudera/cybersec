package com.cloudera.service.common.request;

import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString
public class ClusterRequest extends AbstractRequest {
    private final String clusterId;
    private final List<String> jobs;

    public ClusterRequest(String requestId, String clusterId, List<String> jobs) {
        super(requestId);
        this.clusterId = clusterId;
        this.jobs = jobs;
    }
}
