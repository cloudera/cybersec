package com.cloudera.service.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseBody {
    private String clusterServiceId;
    private String jobIdHex;
    private Map<String, String> jobConfigs;
    private List<Job> jobs;
    private Map<String, String> errorMessage;
    private ClusterMeta clusterMeta;
}
