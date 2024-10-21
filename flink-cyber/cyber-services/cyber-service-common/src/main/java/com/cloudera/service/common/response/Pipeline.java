package com.cloudera.service.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pipeline {
    String id;
    String name;
    String clusterName;
    String date;
    String userName;
}
