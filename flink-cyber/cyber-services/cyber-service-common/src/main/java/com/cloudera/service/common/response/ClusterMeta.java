package com.cloudera.service.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterMeta {
    private String name;
    private String clusterId;
    private String clusterStatus;
    private String version;
    private String flinkVersion;
}
