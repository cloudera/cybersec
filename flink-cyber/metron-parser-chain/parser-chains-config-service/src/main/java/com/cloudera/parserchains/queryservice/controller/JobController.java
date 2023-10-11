package com.cloudera.parserchains.queryservice.controller;

import com.cloudera.parserchains.queryservice.common.ApplicationConstants;
import com.cloudera.parserchains.queryservice.common.exception.FailedAllClusterReponseException;
import com.cloudera.parserchains.queryservice.common.exception.FailedClusterReponseException;
import com.cloudera.parserchains.queryservice.model.enums.JobActions;
import com.cloudera.parserchains.queryservice.service.JobService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

/**
 * The controller responsible for operations with cluster to run and stop cyber jobs on the clusters.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = ApplicationConstants.API_BASE_URL + ApplicationConstants.API_JOBS)
public class JobController {

    private final JobService jobService;

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

    @PostMapping("/config/{clusterId}/{jobIdHex}")
    public ResponseBody updateJobConfig(@ApiParam(name = "clusterId", value = "The ID of the cluster to update config on.", required = true)
                                        @PathVariable("clusterId") String clusterId,
                                        @Schema(name = "jobIdHex", description = "Job ID to update config of", allowableValues = {JobActions.Constants.START_VALUE, JobActions.Constants.RESTART_VALUE, JobActions.Constants.STOP_VALUE, JobActions.Constants.STATUS_VALUE, JobActions.Constants.GET_CONFIG_VALUE, JobActions.Constants.UPDATE_CONFIG_VALUE}, required = true)
                                        @PathVariable("jobIdHex") String jobIdHex,
                                        @RequestParam("config") MultipartFile config) throws FailedClusterReponseException, IOException {
        final String filename = config.getOriginalFilename();
        if (filename == null || !filename.endsWith(".tar.gz")){
            throw new RuntimeException("You should provide config as a .tar.gz archive");
        }
        //Kafka message body should be less than 1Mb, but we should fit a base64 encoded file + other JSON fields.
        if (Math.ceil(config.getSize() / 3d) * 4 >= 1000000){
            throw new RuntimeException("Provided file should be less than ~750Kb in size.");
        }

        final com.cloudera.service.common.request.RequestBody requestBody =
                com.cloudera.service.common.request.RequestBody.builder()
//                        .clusterServiceId() FIXME what it's for and should we set it?
                        .jobIdHex(jobIdHex)
//                        .pipelineDir() FIXME get from jobIdHex?
                        .payload(Base64.getEncoder().encode(config.getBytes()))
                        .build();
        return jobService.makeRequest(clusterId, requestBody, JobActions.Constants.UPDATE_CONFIG_VALUE);
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
