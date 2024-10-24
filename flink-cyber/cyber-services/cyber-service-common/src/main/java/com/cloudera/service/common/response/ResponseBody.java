package com.cloudera.service.common.response;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseBody {
    private Map<String, String> jobConfigs;
    private List<Job> jobs;
    private Map<String, String> errorMessage;
    private ClusterMeta clusterMeta;
}
