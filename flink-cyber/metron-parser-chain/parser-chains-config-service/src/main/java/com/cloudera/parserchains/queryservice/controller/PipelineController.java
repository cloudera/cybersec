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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @Operation(summary = "Retrieve all Pipelines", description = "Finds and returns a set of all available pipeline identifiers.")
    @ApiResponse(responseCode = "200", description = "A list of all pipelines.")
    @ApiResponse(responseCode = "404", description = "No pipelines found, empty set returned.")
    @GetMapping
    public ResponseEntity<Set<String>> findAll() throws IOException {
        Map<String, PipelineResult> pipelineMap = pipelineService.findAll();
        if (pipelineMap == null || pipelineMap.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(pipelineMap.keySet());
    }

    @Operation(summary = "Create a New Pipeline", description = "Creates a new pipeline with the specified name and returns the updated list of all pipelines.")
    @ApiResponse(responseCode = "200", description = "Successfully created the pipeline and returned the new list of pipelines.")
    @PostMapping("/{pipelineName}")
    public ResponseEntity<Set<String>> createPipeline(
            @Parameter(description = "The name of the pipeline to create") @PathVariable String pipelineName) throws IOException {
        PipelineResult newPipeline = pipelineService.createPipeline(pipelineName);
        if (newPipeline != null) {
            return findAll();
        }
        return ResponseEntity.badRequest().build();
    }

    @Operation(summary = "Rename an Existing Pipeline", description = "Renames an existing pipeline specified by its current name to a new name, and returns the updated list of all pipelines.")
    @ApiResponse(responseCode = "200", description = "Successfully renamed the pipeline and returned the new list of pipelines.")
    @PutMapping("/{pipelineName}")
    public ResponseEntity<Set<String>> renamePipeline(
            @Parameter(description = "The current name of the pipeline to be renamed") @PathVariable String pipelineName,
            @Parameter(description = "The new name for the pipeline") @RequestParam String newName) throws IOException {
        PipelineResult updatedPipeline = pipelineService.renamePipeline(pipelineName, newName);
        if (updatedPipeline != null) {
            return findAll();
        }
        return ResponseEntity.badRequest().build();
    }

    @Operation(summary = "Delete an Existing Pipeline", description = "Deletes the pipeline specified by name and returns the updated list of all pipelines if successful.")
    @ApiResponse(responseCode = "200", description = "Successfully deleted the pipeline and returned the new list of pipelines.")
    @DeleteMapping("/{pipelineName}")
    public ResponseEntity<Set<String>> deletePipeline(
            @Parameter(description = "The name of the pipeline to be deleted") @PathVariable String pipelineName) throws IOException {
        boolean pipelineDeleted = pipelineService.deletePipeline(pipelineName);
        if (pipelineDeleted) {
            return findAll();
        }
        return ResponseEntity.badRequest().build();
    }
}
