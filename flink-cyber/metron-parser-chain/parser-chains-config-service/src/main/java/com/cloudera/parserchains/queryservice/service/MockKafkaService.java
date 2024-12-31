package com.cloudera.parserchains.queryservice.service;

import com.cloudera.service.common.request.RequestBody;
import com.cloudera.service.common.request.RequestType;
import com.cloudera.service.common.response.ClusterMeta;
import com.cloudera.service.common.response.Job;
import com.cloudera.service.common.response.ResponseBody;
import com.cloudera.service.common.response.ResponseType;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.flink.api.common.JobID;
import org.apache.flink.api.common.JobStatus;

import java.util.List;
import java.util.stream.Collectors;

public class MockKafkaService implements KafkaServiceInterface {
    private final ClusterMeta cluster1 = ClusterMeta.builder()
            .clusterId("1")
            .name("Cluster1")
            .flinkVersion("3.2.1")
            .version("1.0.1")
            .clusterStatus("online")
            .build();
    private final ClusterMeta cluster2 = ClusterMeta.builder()
            .clusterId("2")
            .name("Cluster2")
            .flinkVersion("3.4.1")
            .version("1.0.5")
            .clusterStatus("online")
            .build();
    private final ClusterMeta cluster3 = ClusterMeta.builder()
            .clusterId("3")
            .name("Cluster3")
            .flinkVersion("3.1.1")
            .version("1.0.4")
            .clusterStatus("offline")
            .build();

    private final Job job1 = Job.builder()
            .jobId(JobID.fromHexString("fd72014d4c864993a2e5a9287b4a9c5d"))
            .confName("job1")
            .jobIdString("job1-id-string")
            .jobBranch("main")
            .jobState(JobStatus.RUNNING)
            .jobFullName("main-job1")
            .jobPipeline("cyb-33")
            .startTime("2024-01-11 10:33:55")
            .jobType(Job.JobType.TRIAGE)
            .user("Cybersec")
            .build();
    private final Job job2 = Job.builder()
            .jobId(JobID.fromHexString("ad72014d4c864993a2e5a9287b4a9c5d"))
            .confName("job2")
            .jobIdString("job2-id-string")
            .jobBranch("main")
            .jobState(JobStatus.RUNNING)
            .jobFullName("main-job2")
            .jobPipeline("cyb-33")
            .startTime("2024-01-14 14:33:55")
            .jobType(Job.JobType.PARSER)
            .user("Cybersec")
            .build();
    private final Job job3 = Job.builder()
            .jobId(JobID.fromHexString("cd72014d4c864993a2e5a9287b4a9c5d"))
            .confName("job3")
            .jobIdString("job3-id-string")
            .jobBranch("main")
            .jobState(JobStatus.RUNNING)
            .jobFullName("main-job3")
            .jobPipeline("cyb-43")
            .startTime("2024-01-11 11:33:55")
            .jobType(Job.JobType.PROFILE)
            .user("Cybersec")
            .build();
    private final Job job4 = Job.builder()
            .jobId(JobID.fromHexString("bd72014d4c864993a2e5a9287b4a9c5d"))
            .confName("job4")
            .jobIdString("job1-id-string")
            .jobBranch("main")
            .jobState(JobStatus.RUNNING)
            .jobFullName("main-job4")
            .jobPipeline("cyb-43")
            .startTime("2024-01-15 15:33:55")
            .jobType(Job.JobType.PARSER)
            .user("Cybersec")
            .build();

    private final ResponseBody responseFromCluster1 = ResponseBody.builder()
            .jobs(Lists.newArrayList(job1, job2, job4))
            .clusterMeta(cluster1)
            .build();
    private final ResponseBody responseFromCluster2 = ResponseBody.builder()
            .jobs(Lists.newArrayList(job1))
            .clusterMeta(cluster2)
            .build();
    private final ResponseBody responseFromCluster3 = ResponseBody.builder()
            .jobs(Lists.newArrayList(job3))
            .clusterMeta(cluster3)
            .build();
    private final List<ResponseBody> responses = Lists.newArrayList(responseFromCluster1, responseFromCluster2, responseFromCluster3);

    @Override
    public Pair<ResponseType, ResponseBody> sendWithReply(RequestType requestType, String clusterId, RequestBody body) {
        if (requestType == RequestType.CREATE_EMPTY_PIPELINE) {
            return Pair.of(ResponseType.CREATE_EMPTY_PIPELINE_RESPONSE, ResponseBody.builder().build());
        }
        if (requestType == RequestType.START_PIPELINE) {
            return Pair.of(ResponseType.START_PIPELINE_RESPONSE, ResponseBody.builder().build());
        }

        if (requestType == RequestType.STOP_JOB_REQUEST) {
            responseFromCluster1.getJobs().remove(0);
            return Pair.of(ResponseType.STOP_JOB_RESPONSE,ResponseBody.builder().build());
        }
        return responses.stream()
                .filter(response -> StringUtils.equalsIgnoreCase(response.getClusterMeta().getName(), clusterId))
                .map(val -> Pair.of(ResponseType.GET_CLUSTER_SERVICE_RESPONSE, val))
                .findFirst()
                .orElse(Pair.of(ResponseType.GET_CLUSTER_SERVICE_RESPONSE, responseFromCluster1));
    }

    @Override
    public List<Pair<ResponseType, ResponseBody>> sendWithReply(RequestType requestType, RequestBody body) {
        return responses.stream()
                .map(val -> Pair.of(ResponseType.GET_ALL_CLUSTERS_SERVICE_RESPONSE, val))
                .collect(Collectors.toList());
    }
}
