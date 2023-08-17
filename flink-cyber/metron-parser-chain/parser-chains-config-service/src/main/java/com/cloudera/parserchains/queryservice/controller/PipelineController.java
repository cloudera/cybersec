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

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * The controller responsible for operations on pipelines.
 */
public interface PipelineController {

  @ApiOperation(value = "Finds and returns all available pipelines.")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "A list of all pipelines."),
      @ApiResponse(code = 404, message = "No valid pipelines found.")
  })
  @GetMapping
  ResponseEntity<Set<String>> findAll() throws IOException;

  @ApiOperation(value = "Allows to create a new pipeline.")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "A new list of all pipelines.")
  })
  @PostMapping("/{pipelineName}")
  ResponseEntity<Set<String>> createPipeline(@PathVariable String pipelineName) throws IOException;


  @ApiOperation(value = "Allows to rename existing pipeline.")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "A new list of all pipelines.")
  })
  @PutMapping("/{pipelineName}")
  ResponseEntity<Set<String>> renamePipeline(@PathVariable String pipelineName,
      @RequestParam String newName) throws IOException;

  @ApiOperation(value = "Allows to delete existing pipeline.")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "A new list of all pipelines.")
  })
  @DeleteMapping("/{pipelineName}")
  ResponseEntity<Set<String>> deletePipeline(@PathVariable String pipelineName) throws IOException;

}
