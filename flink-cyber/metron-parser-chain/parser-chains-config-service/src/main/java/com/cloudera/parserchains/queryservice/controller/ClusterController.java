package com.cloudera.parserchains.queryservice.controller;

import com.cloudera.parserchains.queryservice.common.ApplicationConstants;
import com.cloudera.parserchains.queryservice.common.exception.FailedAllClusterReponseException;
import com.cloudera.parserchains.queryservice.common.exception.FailedClusterReponseException;
import com.cloudera.parserchains.queryservice.service.ClusterService;
import com.cloudera.service.common.response.ResponseBody;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
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

    @ApiOperation(value = "Retrieves information about all cluster services.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "A list of all clusters.")
    })
    @GetMapping
    public List<ResponseBody> getAllServices() throws FailedAllClusterReponseException {
        return clusterService.getAllClusterInfo();
    }

    @ApiOperation(value = "Retrieves information about a cluster with specified id.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "A response with cluster information."),
            @ApiResponse(code = 404, message = "The cluster does not exist.")

    })
    @GetMapping(value = "/{id}")
    public ResponseBody getClusterService(
            @ApiParam(name = "id", value = "The ID of the cluster to retrieve.", required = true)
            @PathVariable("id") String clusterId) throws FailedClusterReponseException {
        return clusterService.getClusterInfo(clusterId);
    }

    @ExceptionHandler(FailedAllClusterReponseException.class)
    protected ResponseEntity<List<ResponseBody>> handleFailedAllClusterRequest(FailedAllClusterReponseException ex) {
        return new ResponseEntity<>(ex.getResponseBodies(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(FailedClusterReponseException.class)
    protected ResponseEntity<ResponseBody> handleFailedClusterRequest(FailedClusterReponseException ex) {
        return new ResponseEntity<>(ex.getResponseBody(), HttpStatus.BAD_REQUEST);
    }
}
