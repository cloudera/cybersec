package com.cloudera.parserchains.queryservice.controller;

import com.cloudera.parserchains.queryservice.common.ApplicationConstants;
import com.cloudera.parserchains.queryservice.common.exception.FailedAllClusterReponseException;
import com.cloudera.parserchains.queryservice.common.exception.FailedClusterReponseException;
import com.cloudera.parserchains.queryservice.service.ClusterService;
import com.cloudera.service.common.response.Pipeline;
import com.cloudera.service.common.response.ResponseBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * The controller responsible for operations with cluster to run and stop cyber jobs on the clusters.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = ApplicationConstants.API_BASE_URL + ApplicationConstants.API_CLUSTERS)
public class ClusterController {

    private final ClusterService clusterService;

    @Operation(description = "Retrieves information about all cluster services.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "A list of all clusters.")
    })
    @GetMapping
    public List<ResponseBody> getAllServices() throws FailedAllClusterReponseException {
        return clusterService.getAllClusterInfo();
    }

    @Operation(description = "Retrieves information about a cluster with specified id.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "A response with cluster information."),
            @ApiResponse(responseCode = "404", description = "The cluster does not exist.")

    })
    @GetMapping(value = "/{id}")
    public ResponseBody getClusterService(
            @Parameter(name = "id", description = "The ID of the cluster to retrieve.", required = true)
            @PathVariable("id") String clusterId) throws FailedClusterReponseException {
        return clusterService.getClusterInfo(clusterId);
    }

    @Operation(description = "Retrieves information about all pipelines on all services.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "A list of all pipelines.")
    })
    @GetMapping(value = "/pipelines")
    public List<Pipeline> getAllPipelines() throws FailedAllClusterReponseException {
        return clusterService.getAllPipelines();
    }

    @Operation(description = "Retrieves information about all pipelines on all services.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "A list of all pipelines.")
    })
    @PostMapping(value = "{clusterId}/pipelines")
    public ResponseBody createEmptyPipeline(
            @RequestBody com.cloudera.service.common.request.RequestBody body,
            @Parameter(name = "clusterId", description = "The ID of the cluster to update config on.", required = true)
            @PathVariable("clusterId") String clusterId
    ) throws FailedClusterReponseException {
        return clusterService.createEmptyPipeline(clusterId, body);
    }

    @Operation(description = "Retrieves information about all pipelines on all services.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "A list of all pipelines.")
    })
    @PostMapping(value = "{clusterId}/pipelines/{name}/start")
    public ResponseBody startPipeline(
            @Parameter(name = "name", description = "The pipeline name to create empty pipeline.", required = true)
            @PathVariable("name") String name,
            @Parameter(name = "clusterId", description = "The ID of the cluster to update config on.", required = true)
            @PathVariable("clusterId") String clusterId,
            @RequestPart("payload") MultipartFile payload,
            @RequestPart("body") com.cloudera.service.common.request.RequestBody body
    ) throws IOException, FailedClusterReponseException {
        return clusterService.startPipelineJob(clusterId, name, body.getBranch(), body.getProfileName(), body.getJobs(), payload.getBytes());
    }

    @Operation(description = "Retrieves information about a pipeline on the cluster with specified id.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "A response with cluster information."),
            @ApiResponse(responseCode = "404", description = "The cluster does not exist.")

    })
    @GetMapping(value = "/{id}/pipelines")
    public List<Pipeline> getClusterPipelines(
            @Parameter(name = "id", description = "The ID of the cluster to retrieve.", required = true)
            @PathVariable("id") String clusterId) throws FailedClusterReponseException {
        return clusterService.getClusterPipelines(clusterService.getClusterInfo(clusterId));
    }
}
