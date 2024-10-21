package com.cloudera.service.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
