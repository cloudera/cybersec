package com.cloudera.parserchains.queryservice.controller;

import com.cloudera.parserchains.core.model.define.ParserChainSchema;
import com.cloudera.parserchains.queryservice.common.ApplicationConstants;
import com.cloudera.parserchains.queryservice.common.exception.FailedAllClusterReponseException;
import com.cloudera.parserchains.queryservice.common.exception.FailedClusterReponseException;
import com.cloudera.parserchains.queryservice.model.enums.JobActions;
import com.cloudera.parserchains.queryservice.service.ClusterService;
import com.cloudera.service.common.response.ResponseBody;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The controller responsible for operations with cluster to run and stop cyber jobs on the clusters.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = ApplicationConstants.API_BASE_URL + ApplicationConstants.API_JOBS)
public class JobController {


    @ApiOperation(value = "Retrieves information about all cluster services.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "A list of all clusters.")
    })
    @PostMapping("/{action}")
    public List<ResponseBody> getJobs(@ApiParam(name = "requestBody", value = "The new parser chain definition.", required = true)
                                      @RequestBody com.cloudera.service.common.request.RequestBody body,
                                      @ApiParam(name = "clusterId", value = "The ID of the cluster to retrieve.", required = true)
                                      @PathVariable("clusterId") String clusterId,
                                      @Schema(name = "action", description = "Jobs action for start stop restart Job", allowableValues = {JobActions.Constants.START_VALUE, JobActions.Constants.RESTART_VALUE, JobActions.Constants.STOP_VALUE, JobActions.Constants.STATUS_VALUE, JobActions.Constants.GET_CONFIG_VALUE, JobActions.Constants.UPDATE_CONFIG_VALUE}, required = true)
                                      @PathVariable("action") String action) throws FailedAllClusterReponseException {
        return null;
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
