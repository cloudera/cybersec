package com.cloudera.service.common.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;


@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
public class ClusterPipelineRequest extends AbstractRequest {
    private final String pipelineName;
    private final String branch;
    private final String profileName;
    private final String parserName;
    private final String gitUrl;
    private final String userName;
    private final String password;
    private final List<String> jobs;

    @JsonCreator
    public ClusterPipelineRequest(
            @JsonProperty("requestId") String requestId,
            @JsonProperty("pipelineName") String pipelineName,
            @JsonProperty("branch") String branch,
            @JsonProperty("profileName") String profileName,
            @JsonProperty("parserName") String parserName,
            @JsonProperty("gitUrl") String gitUrl,
            @JsonProperty("userName") String userName,
            @JsonProperty("password") String password,
            @JsonProperty("jobs") List<String> jobs) {
        super(requestId);
        this.pipelineName = pipelineName;
        this.branch = branch;
        this.profileName = profileName;
        this.gitUrl = gitUrl;
        this.userName = userName;
        this.password = password;
        this.jobs = jobs;
        this.parserName = parserName;
    }
}
