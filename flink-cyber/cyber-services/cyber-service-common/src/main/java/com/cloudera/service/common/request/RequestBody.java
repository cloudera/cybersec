package com.cloudera.service.common.request;

import java.util.List;
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
    private String pipelineName;
    private String branch;
    private String profileName;
    private String parserName;
    private List<String> jobs;
    private byte[] payload;
}
