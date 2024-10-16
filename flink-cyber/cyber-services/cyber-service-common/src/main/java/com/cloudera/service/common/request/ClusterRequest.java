package com.cloudera.service.common.request;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString
public class ClusterRequest extends AbstractRequest{
    private final String clusterId;
    private final List<String> jobs;

    public ClusterRequest(String requestId, String clusterId, List<String> jobs) {
        super(requestId);
        this.clusterId = clusterId;
        this.jobs = jobs;
    }
}
