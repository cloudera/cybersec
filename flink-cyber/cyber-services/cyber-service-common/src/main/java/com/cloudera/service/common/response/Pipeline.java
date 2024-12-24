package com.cloudera.service.common.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pipeline {
    private String id;
    private String name;
    private String clusterName;
    private String date;
    private List<String> jobs;
    private String userName;
}
