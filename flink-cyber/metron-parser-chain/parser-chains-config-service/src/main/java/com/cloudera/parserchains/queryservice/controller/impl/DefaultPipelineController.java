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

package com.cloudera.parserchains.queryservice.controller.impl;

import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.PIPELINE_BASE_URL;
import com.cloudera.parserchains.queryservice.controller.PipelineController;
import com.cloudera.parserchains.queryservice.model.exec.PipelineResult;
import com.cloudera.parserchains.queryservice.service.PipelineService;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = PIPELINE_BASE_URL)
public class DefaultPipelineController implements PipelineController {

  private final PipelineService pipelineService;

  public ResponseEntity<Set<String>> findAll() throws IOException {
    Map<String, PipelineResult> pipelineMap = pipelineService.findAll();
    if (pipelineMap == null || pipelineMap.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(pipelineMap.keySet());
  }

  public ResponseEntity<Set<String>> createPipeline(String pipelineName) throws IOException {
    PipelineResult newPipeline = pipelineService.createPipeline(pipelineName);
    if (newPipeline != null) {
      return findAll();
    }
    return ResponseEntity.badRequest().build();
  }

  public ResponseEntity<Set<String>> renamePipeline(String pipelineName, String newName) throws IOException {
    PipelineResult updatedPipeline = pipelineService.renamePipeline(pipelineName, newName);
    if (updatedPipeline != null) {
      return findAll();
    }
    return ResponseEntity.badRequest().build();
  }

  public ResponseEntity<Set<String>> deletePipeline(String pipelineName) throws IOException {
    boolean pipelineDeleted = pipelineService.deletePipeline(pipelineName);
    if (pipelineDeleted) {
      return findAll();
    }
    return ResponseEntity.badRequest().build();
  }

}
