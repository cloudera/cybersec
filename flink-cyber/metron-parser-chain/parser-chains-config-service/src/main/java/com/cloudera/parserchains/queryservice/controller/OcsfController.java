package com.cloudera.parserchains.queryservice.controller;

import com.cloudera.parserchains.queryservice.common.ApplicationConstants;
import com.cloudera.parserchains.queryservice.service.OcsfService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The controller responsible for operations with cluster to run and stop cyber jobs on the clusters.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = ApplicationConstants.OCSF_BASE_URL)
public class OcsfController {

    private final OcsfService ocsfService;

    @GetMapping
    public String getOcsfSchema() {
        return ocsfService.getOcsfSchemaString();
    }

}
