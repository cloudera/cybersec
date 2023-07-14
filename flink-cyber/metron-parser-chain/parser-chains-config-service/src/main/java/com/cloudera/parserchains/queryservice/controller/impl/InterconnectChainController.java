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

import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.PARSER_CONFIG_PROXY_BASE_URL;
import com.cloudera.parserchains.core.model.define.ParserChainSchema;
import com.cloudera.parserchains.core.utils.JSONUtils;
import com.cloudera.parserchains.queryservice.common.ApplicationConstants;
import com.cloudera.parserchains.queryservice.controller.ChainController;
import com.cloudera.parserchains.queryservice.model.enums.KafkaMessageType;
import com.cloudera.parserchains.queryservice.model.exec.ChainTestRequest;
import com.cloudera.parserchains.queryservice.model.exec.ChainTestResponse;
import com.cloudera.parserchains.queryservice.model.summary.ParserChainSummary;
import com.cloudera.parserchains.queryservice.service.KafkaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.type.CollectionType;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = PARSER_CONFIG_PROXY_BASE_URL)
public class InterconnectChainController implements ChainController {

  private static final ObjectMapper MAPPER = JSONUtils.INSTANCE.getMapper();
  private static final ObjectReader CHAIN_SUMMARY_READER = MAPPER.readerFor(MAPPER.getTypeFactory()
        .constructCollectionType(List.class, ParserChainSummary.class));
  private static final ObjectReader CHAIN_SCHEMA_READER = MAPPER.readerFor(ParserChainSchema.class);
  private static final ObjectReader CHAIN_TEST_READER = MAPPER.readerFor(ChainTestResponse.class);

  private final KafkaService kafkaService;

  public ResponseEntity<List<ParserChainSummary>> findAll(String pipelineName, String clusterId) throws IOException {
    final Map<String, Object> params = new HashMap<>();
    params.put(ApplicationConstants.PIPELINE_NAME_PARAM, pipelineName);
    return kafkaService.sendWithReply(KafkaMessageType.CHAIN_FIND_ALL, clusterId, params, CHAIN_SUMMARY_READER);
  }

  public ResponseEntity<ParserChainSchema> create(String pipelineName, String clusterId, ParserChainSchema chain)
      throws IOException {
    final Map<String, Object> params = new HashMap<>();
    params.put(ApplicationConstants.PIPELINE_NAME_PARAM, pipelineName);
    params.put(ApplicationConstants.CHAIN_PARAM, chain);

    return kafkaService.sendWithReply(KafkaMessageType.CHAIN_CREATE, clusterId, params, CHAIN_SCHEMA_READER);
  }

  public ResponseEntity<ParserChainSchema> read(String pipelineName, String clusterId, String id) throws IOException {
    final Map<String, Object> params = new HashMap<>();
    params.put(ApplicationConstants.PIPELINE_NAME_PARAM, pipelineName);
    params.put(ApplicationConstants.ID_PARAM, id);

    return kafkaService.sendWithReply(KafkaMessageType.CHAIN_READ, clusterId, params, CHAIN_SCHEMA_READER);
  }

  public ResponseEntity<ParserChainSchema> update(String pipelineName, String clusterId,
      ParserChainSchema chain, String id) throws IOException {
    final Map<String, Object> params = new HashMap<>();
    params.put(ApplicationConstants.PIPELINE_NAME_PARAM, pipelineName);
    params.put(ApplicationConstants.CHAIN_PARAM, chain);
    params.put(ApplicationConstants.ID_PARAM, id);

    return kafkaService.sendWithReply(KafkaMessageType.CHAIN_UPDATE, clusterId, params, CHAIN_SCHEMA_READER);
  }

  public ResponseEntity<Void> delete(String pipelineName, String clusterId, String id) throws IOException {
    final Map<String, Object> params = new HashMap<>();
    params.put(ApplicationConstants.PIPELINE_NAME_PARAM, pipelineName);
    params.put(ApplicationConstants.ID_PARAM, id);

    return kafkaService.sendWithReply(KafkaMessageType.CHAIN_DELETE, clusterId, params, null);
  }

  public ResponseEntity<ChainTestResponse> test(String clusterId, ChainTestRequest testRun) throws IOException {
    final Map<String, Object> params = new HashMap<>();
    params.put(ApplicationConstants.TEST_RUN_PARAM, testRun);

    return kafkaService.sendWithReply(KafkaMessageType.CHAIN_TEST, clusterId, params, CHAIN_TEST_READER);
  }

}
