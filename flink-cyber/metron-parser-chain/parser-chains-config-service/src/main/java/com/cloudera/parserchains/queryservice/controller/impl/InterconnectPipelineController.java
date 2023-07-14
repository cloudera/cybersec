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

import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.PIPELINE_PROXY_BASE_URL;
import com.cloudera.parserchains.core.model.define.ParserID;
import com.cloudera.parserchains.core.utils.JSONUtils;
import com.cloudera.parserchains.queryservice.controller.PipelineController;
import com.cloudera.parserchains.queryservice.model.describe.ParserDescriptor;
import com.cloudera.parserchains.queryservice.model.enums.KafkaMessageType;
import com.cloudera.parserchains.queryservice.model.summary.ParserSummary;
import com.cloudera.parserchains.queryservice.service.KafkaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = PIPELINE_PROXY_BASE_URL)
public class InterconnectPipelineController implements PipelineController {

  private static final ObjectMapper MAPPER = JSONUtils.INSTANCE.getMapper();
  private static final ObjectReader PIPELINE_LIST_READER = MAPPER.readerFor(MAPPER.getTypeFactory()
      .constructCollectionType(Set.class, String.class));

  private final KafkaService kafkaService;

  public ResponseEntity<Set<String>> findAll(String clusterId) throws IOException {
    return kafkaService.sendWithReply(KafkaMessageType.PIPELINES_FIND_ALL, clusterId, null, PIPELINE_LIST_READER);
  }

}
