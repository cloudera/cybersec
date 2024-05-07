package com.cloudera.parserchains.queryservice.controller;

import com.cloudera.parserchains.queryservice.common.ApplicationConstants;
import com.cloudera.parserchains.queryservice.common.exception.FailedClusterReponseException;
import com.cloudera.parserchains.queryservice.model.enums.JobActions;
import com.cloudera.parserchains.queryservice.service.JobService;
import com.cloudera.service.common.response.ResponseBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

/**
 * The controller responsible for operations with cluster to run and stop cyber jobs on the clusters.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = ApplicationConstants.API_BASE_URL + ApplicationConstants.API_JOBS)
public class JobController {
    private final JobService jobService;
    @Value("${upload.file.max.size:1000000}")
    private Integer uploadFileMaxSize;

    @Operation(description = "Retrieves information about all cluster services.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "A list of all clusters.")
    })
    @PostMapping("/{action}")
    public ResponseBody executeAction(@Parameter(name = "requestBody", description = "The new parser chain definition.", required = true)
                                      @RequestBody com.cloudera.service.common.request.RequestBody body,
                                      @Parameter(name = "clusterId", description = "The ID of the cluster to retrieve.", required = true)
                                      @PathVariable("clusterId") String clusterId,
                                      @Schema(name = "action", description = "Jobs action for start stop restart Job", allowableValues = {JobActions.Constants.START_VALUE, JobActions.Constants.RESTART_VALUE, JobActions.Constants.STOP_VALUE, JobActions.Constants.STATUS_VALUE, JobActions.Constants.GET_CONFIG_VALUE, JobActions.Constants.UPDATE_CONFIG_VALUE}, required = true)
                                      @PathVariable("action") String action) throws FailedClusterReponseException {
        return jobService.makeRequest(clusterId, body, action);
    }

    @PostMapping("/config/{pipeline}/{jobIdHex}")
    public ResponseBody updateJobConfig(@Parameter(name = "clusterId", description = "The ID of the cluster to update config on.", required = true)
                                        @PathVariable("clusterId") String clusterId,
                                        @Schema(name = "pipeline", description = "Pipeline in which the job is running", allowableValues = {JobActions.Constants.START_VALUE, JobActions.Constants.RESTART_VALUE, JobActions.Constants.STOP_VALUE, JobActions.Constants.STATUS_VALUE, JobActions.Constants.GET_CONFIG_VALUE, JobActions.Constants.UPDATE_CONFIG_VALUE}, required = true)
                                        @PathVariable("pipeline") String pipeline,
                                        @Schema(name = "jobIdHex", description = "Job ID to update config of", allowableValues = {JobActions.Constants.START_VALUE, JobActions.Constants.RESTART_VALUE, JobActions.Constants.STOP_VALUE, JobActions.Constants.STATUS_VALUE, JobActions.Constants.GET_CONFIG_VALUE, JobActions.Constants.UPDATE_CONFIG_VALUE}, required = true)
                                        @PathVariable("jobIdHex") String jobIdHex,
                                        @RequestParam("config") MultipartFile config) throws FailedClusterReponseException, IOException {
        final String filename = config.getOriginalFilename();
        if (filename == null || !filename.endsWith(".tar.gz")){
            throw new RuntimeException("You should provide config as a .tar.gz archive");
        }
        //Kafka message body should be less than 1Mb, but we should fit a base64 encoded file + other JSON fields.
        if (Math.ceil(config.getSize() / 3d) * 4 >= uploadFileMaxSize){
            throw new RuntimeException("Provided file should be less than ~750Kb in size.");
        }
        final com.cloudera.service.common.request.RequestBody requestBody =
                com.cloudera.service.common.request.RequestBody.builder()
                        .clusterServiceId(clusterId)
                        .jobIdHex(jobIdHex)
                        .payload(Base64.getEncoder().encode(config.getBytes()))
                        .build();
        return jobService.makeRequest(clusterId, requestBody, JobActions.Constants.UPDATE_CONFIG_VALUE);
    }


}
