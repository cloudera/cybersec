/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

package com.cloudera.parserchains.queryservice.controller;

import com.cloudera.parserchains.queryservice.service.PipelineService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.apache.flink.core.fs.Path;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.PIPELINE_BASE_URL;

/**
 * The controller responsible for operations on pipelines.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = PIPELINE_BASE_URL)
public class PipelineController {
    private final PipelineService pipelineService;

    @ApiOperation(value = "Finds and returns all available pipelines.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "A list of all pipelines."),
            @ApiResponse(code = 404, message = "No valid pipelines found.")
    })
    @GetMapping
    public ResponseEntity<Set<String>> findAll() throws IOException {
        Map<String, Path> pipelineMap = pipelineService.findAll();
        if (pipelineMap == null || pipelineMap.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(pipelineMap.keySet());
    }

    @ApiOperation(value = "Allows to create a new pipeline.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "A new list of all pipelines.")
    })
    @PostMapping("/{pipelineName}")
    public ResponseEntity<Set<String>> createPipeline(@PathVariable String pipelineName) {
        Set<String> pipelineList = pipelineService.createPipeline(pipelineName);
        return ResponseEntity.ok(pipelineList);
    }
}
