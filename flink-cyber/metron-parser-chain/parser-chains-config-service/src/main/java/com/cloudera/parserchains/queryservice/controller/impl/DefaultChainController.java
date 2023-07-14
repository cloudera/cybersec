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

import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.API_CHAINS_READ_URL;
import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.PARSER_CONFIG_BASE_URL;
import com.cloudera.parserchains.core.ChainLink;
import com.cloudera.parserchains.core.InvalidParserException;
import com.cloudera.parserchains.core.model.define.ParserChainSchema;
import com.cloudera.parserchains.queryservice.config.AppProperties;
import com.cloudera.parserchains.queryservice.controller.ChainController;
import com.cloudera.parserchains.queryservice.model.exec.ChainTestRequest;
import com.cloudera.parserchains.queryservice.model.exec.ChainTestResponse;
import com.cloudera.parserchains.queryservice.model.exec.ParserResult;
import com.cloudera.parserchains.queryservice.model.exec.ResultLog;
import com.cloudera.parserchains.queryservice.model.summary.ParserChainSummary;
import com.cloudera.parserchains.queryservice.service.ChainBuilderService;
import com.cloudera.parserchains.queryservice.service.ChainExecutorService;
import com.cloudera.parserchains.queryservice.service.ChainPersistenceService;
import com.cloudera.parserchains.queryservice.service.PipelineService;
import com.cloudera.parserchains.queryservice.service.ResultLogBuilder;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.core.fs.Path;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(value = PARSER_CONFIG_BASE_URL)
@RequiredArgsConstructor
public class DefaultChainController implements ChainController {

  /**
   * The maximum number of sample text values that can be used to test a parser chain.
   */
  static final int MAX_SAMPLES_PER_TEST = 200;

  private final ChainPersistenceService chainPersistenceService;

  private final ChainBuilderService chainBuilderService;

  private final ChainExecutorService chainExecutorService;

  private final PipelineService pipelineService;

  private final AppProperties appProperties;

  public ResponseEntity<List<ParserChainSummary>> findAll(String pipelineName, String clusterId) throws IOException {
    String configPath = getConfigPath(pipelineName);

    List<ParserChainSummary> configs = chainPersistenceService.findAll(Paths.get(configPath));
    return ResponseEntity.ok(configs);
  }

  public ResponseEntity<ParserChainSchema> create(String pipelineName, String clusterId, ParserChainSchema chain)
      throws IOException {
    String configPath = getConfigPath(pipelineName);

    ParserChainSchema createdChain = chainPersistenceService.create(chain, Paths.get(configPath));
    if (null == createdChain) {
      return ResponseEntity.notFound().build();
    } else {
      return ResponseEntity
          .created(URI.create(API_CHAINS_READ_URL.replace("{id}", createdChain.getId())))
          .body(createdChain);
    }
  }

  public ResponseEntity<ParserChainSchema> read(String pipelineName, String clusterId, String id) throws IOException {
    String configPath = getConfigPath(pipelineName);

    ParserChainSchema chain = chainPersistenceService.read(id, Paths.get(configPath));
    if (null == chain) {
      return ResponseEntity.notFound().build();
    } else {
      return ResponseEntity.ok(chain);
    }
  }

  public ResponseEntity<ParserChainSchema> update(String pipelineName, String clusterId,
      ParserChainSchema chain, String id) throws IOException {
    String configPath = getConfigPath(pipelineName);

    try {
      ParserChainSchema updatedChain = chainPersistenceService.update(id, chain, Paths.get(configPath));
      if (null == updatedChain) {
        return ResponseEntity.notFound().build();
      } else {
        return ResponseEntity.noContent().build();
      }
    } catch (IOException ioe) {
      throw new RuntimeException("Unable to update configuration with id=" + id);
    }
  }

  public ResponseEntity<Void> delete(String pipelineName, String clusterId, String id) throws IOException {
    String configPath = getConfigPath(pipelineName);

    if (chainPersistenceService.delete(id, Paths.get(configPath))) {
      return ResponseEntity.noContent().build();
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  public ResponseEntity<ChainTestResponse> test(String clusterId, ChainTestRequest testRun) {
    ParserChainSchema chain = testRun.getParserChainSchema();
    ChainTestResponse results = new ChainTestResponse();
    testRun.getSampleData().getSource()
        .stream()
        .limit(MAX_SAMPLES_PER_TEST)
        .map(sample -> doTest(chain, sample))
        .forEach(result -> results.addResult(result));
    return ResponseEntity.ok(results);
  }

  /**
   * Parse sample text using a parser chain.
   *
   * @param schema      Defines the parser chain that needs to be constructed.
   * @param textToParse The text to parse.
   * @return
   */
  private ParserResult doTest(ParserChainSchema schema, String textToParse) {
    ParserResult result;
    try {
      ChainLink chain = chainBuilderService.build(schema);
      result = chainExecutorService.execute(chain, textToParse);

    } catch (InvalidParserException e) {
      log.info("The parser chain is invalid as constructed.", e);
      ResultLog log = ResultLogBuilder.error()
          .parserId(e.getBadParser().getLabel())
          .exception(e)
          .build();
      result = new ParserResult()
          .setLog(log);
    }
    return result;
  }

  private String getConfigPath(String pipelineName) throws IOException {
    if (!StringUtils.hasText(pipelineName)) {
      return appProperties.getConfigPath();
    }

    return Optional.ofNullable(pipelineService.findAll())
        .map(pipelineMap -> pipelineMap.get(pipelineName))
        .map(Path::getPath)
        .orElseGet(appProperties::getConfigPath);
  }
}
