package com.cloudera.parserchains.queryservice.controller;

import com.cloudera.parserchains.queryservice.common.ApplicationConstants;
import com.cloudera.parserchains.queryservice.common.exception.FailedAllClusterReponseException;
import com.cloudera.parserchains.queryservice.common.exception.FailedClusterReponseException;
import com.cloudera.parserchains.queryservice.service.ClusterService;
import com.cloudera.service.common.response.ResponseBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The controller responsible for operations with cluster to run and stop cyber jobs on the clusters.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = ApplicationConstants.API_BASE_URL + ApplicationConstants.API_CLUSTERS)
public class ClusterController {

    private final ClusterService clusterService;

    @Operation(summary = "Retrieves information about all cluster services", description = "Returns a list of all clusters along with their respective services and statuses.")
    @ApiResponse(responseCode = "200", description = "A list of all clusters.")
    @GetMapping
    public List<ResponseBody> getAllServices() throws FailedAllClusterReponseException {
        return clusterService.getAllClusterInfo();
    }

    @Operation(summary = "Retrieves information about a specific cluster", description = "Returns detailed information about a cluster identified by the given ID.")
    @ApiResponse(responseCode = "200", description = "A response with detailed cluster information.")
    @ApiResponse(responseCode = "404", description = "The cluster does not exist.")
    @GetMapping(value = "/{id}")
    public ResponseBody getClusterService(
            @Parameter(description = "The ID of the cluster to retrieve.", required = true) @PathVariable("id") String clusterId) throws FailedClusterReponseException {
        return clusterService.getClusterInfo(clusterId);
    }
}
