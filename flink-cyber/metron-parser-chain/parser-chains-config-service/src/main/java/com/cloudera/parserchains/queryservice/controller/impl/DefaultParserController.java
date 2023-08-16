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

import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.PARSER_CONFIG_BASE_URL;
import com.cloudera.parserchains.core.model.define.ParserID;
import com.cloudera.parserchains.queryservice.controller.ParserController;
import com.cloudera.parserchains.queryservice.model.describe.ParserDescriptor;
import com.cloudera.parserchains.queryservice.model.summary.ParserSummary;
import com.cloudera.parserchains.queryservice.service.ParserDiscoveryService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = PARSER_CONFIG_BASE_URL)
public class DefaultParserController implements ParserController {

  private final ParserDiscoveryService parserDiscoveryService;

  public ResponseEntity<List<ParserSummary>> findAll() throws IOException {
    List<ParserSummary> types = parserDiscoveryService.findAll();
    return ResponseEntity.ok(types);
  }

  public ResponseEntity<Map<ParserID, ParserDescriptor>> describeAll() throws IOException {
    Map<ParserID, ParserDescriptor> configs = parserDiscoveryService.describeAll();
    if (configs != null || configs.size() >= 0) {
      return ResponseEntity.ok(configs);
    } else {
      return ResponseEntity.notFound().build();
    }
  }
}
