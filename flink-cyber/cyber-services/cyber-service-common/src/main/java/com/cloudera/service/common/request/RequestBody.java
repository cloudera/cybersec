package com.cloudera.service.common.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestBody {
    private String clusterServiceId;
    private String jobIdHex;
    private String pipelineDir;
    private String branch;
    private byte[] payload;
}
