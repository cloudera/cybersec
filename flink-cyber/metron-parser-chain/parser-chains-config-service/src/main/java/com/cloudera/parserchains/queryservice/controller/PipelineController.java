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

import com.cloudera.parserchains.queryservice.model.exec.PipelineResult;
import com.cloudera.parserchains.queryservice.service.PipelineService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    @PreAuthorize("@spnegoUserDetailsService.hasAccess('get', '*')")
    public ResponseEntity<Set<String>> findAll() throws IOException {
        Map<String, PipelineResult> pipelineMap = pipelineService.findAll();
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
    @PreAuthorize("@spnegoUserDetailsService.hasAccess('post', '*')")
    public ResponseEntity<Set<String>> createPipeline(@PathVariable String pipelineName) throws IOException {
        PipelineResult newPipeline = pipelineService.createPipeline(pipelineName);
        if (newPipeline != null) {
            return findAll();
        }
        return ResponseEntity.badRequest().build();
    }

    @ApiOperation(value = "Allows to rename existing pipeline.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "A new list of all pipelines.")
    })
    @PutMapping("/{pipelineName}")
    @PreAuthorize("@spnegoUserDetailsService.hasAccess('put', '*')")
    public ResponseEntity<Set<String>> renamePipeline(@PathVariable String pipelineName,
                                                      @RequestParam String newName) throws IOException {
        PipelineResult updatedPipeline = pipelineService.renamePipeline(pipelineName, newName);
        if (updatedPipeline != null) {
            return findAll();
        }
        return ResponseEntity.badRequest().build();
    }

    @ApiOperation(value = "Allows to delete existing pipeline.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "A new list of all pipelines.")
    })
    @DeleteMapping("/{pipelineName}")
    @PreAuthorize("@spnegoUserDetailsService.hasAccess('delete', '*')")
    public ResponseEntity<Set<String>> deletePipeline(@PathVariable String pipelineName) throws IOException {
        boolean pipelineDeleted = pipelineService.deletePipeline(pipelineName);
        if (pipelineDeleted) {
            return findAll();
        }
        return ResponseEntity.badRequest().build();
    }

}
