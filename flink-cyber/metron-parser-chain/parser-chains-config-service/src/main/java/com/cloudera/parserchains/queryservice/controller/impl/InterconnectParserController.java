/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.cloudera.parserchains.queryservice.controller.impl;

import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.PARSER_CONFIG_PROXY_BASE_URL;
import com.cloudera.parserchains.core.model.define.ParserChainSchema;
import com.cloudera.parserchains.core.model.define.ParserID;
import com.cloudera.parserchains.core.utils.JSONUtils;
import com.cloudera.parserchains.queryservice.controller.ParserController;
import com.cloudera.parserchains.queryservice.model.describe.ParserDescriptor;
import com.cloudera.parserchains.queryservice.model.enums.KafkaMessageType;
import com.cloudera.parserchains.queryservice.model.exec.ChainTestResponse;
import com.cloudera.parserchains.queryservice.model.summary.ParserChainSummary;
import com.cloudera.parserchains.queryservice.model.summary.ParserSummary;
import com.cloudera.parserchains.queryservice.service.KafkaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = PARSER_CONFIG_PROXY_BASE_URL)
public class InterconnectParserController implements ParserController {

  private static final ObjectMapper MAPPER = JSONUtils.INSTANCE.getMapper();
  private static final ObjectReader PARSER_SUMMARY_READER = MAPPER.readerFor(MAPPER.getTypeFactory()
      .constructCollectionType(List.class, ParserSummary.class));
  private static final ObjectReader PARSER_DESCRIPTOR_MAP_READER = MAPPER.readerFor(MAPPER.getTypeFactory()
      .constructMapType(Map.class, ParserID.class, ParserDescriptor.class));

  private final KafkaService kafkaService;

  public ResponseEntity<List<ParserSummary>> findAll(String clusterId) throws IOException {
    return kafkaService.sendWithReply(KafkaMessageType.PARSER_FIND_ALL, clusterId, null, PARSER_SUMMARY_READER);
  }

  public ResponseEntity<Map<ParserID, ParserDescriptor>> describeAll(String clusterId) throws IOException {
    return kafkaService.sendWithReply(KafkaMessageType.PARSER_DESCRIBE_ALL, clusterId, null, PARSER_DESCRIPTOR_MAP_READER);
  }
}
