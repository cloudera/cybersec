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

import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.PIPELINE_BASE_URL;
import com.cloudera.parserchains.queryservice.model.enums.KafkaMessageType;
import com.cloudera.parserchains.queryservice.service.KafkaService;
import com.cloudera.parserchains.queryservice.service.PipelineService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.core.fs.Path;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The controller responsible for operations on parser chains.
 */
@Slf4j
@RestController
@RequestMapping(value = PIPELINE_BASE_URL)
@RequiredArgsConstructor
public class PipelineController {

  private final PipelineService pipelineService;
  private final KafkaService kafkaService;

  @ApiOperation(value = "Finds and returns all available pipelines.")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "A list of all pipelines."),
      @ApiResponse(code = 404, message = "No valid pipelines found.")
  })
  @GetMapping
  ResponseEntity<Set<String>> findAll() throws IOException {
    Map<String, Path> pipelineMap = pipelineService.findAll();
    if (pipelineMap == null || pipelineMap.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(pipelineMap.keySet());
  }

  //TODO remove once testing is done
  @GetMapping("/test/parser")
  public String testParser() {
    return kafkaService.sendWithReply("clusterId1", KafkaMessageType.PARSER, "test-parser-value");
  }

  //TODO remove once testing is done
  @GetMapping("/test/cluster")
  public String testCluster() {
    return kafkaService.sendWithReply("clusterId1", KafkaMessageType.CLUSTER, "test-cluster-value");
  }
}
