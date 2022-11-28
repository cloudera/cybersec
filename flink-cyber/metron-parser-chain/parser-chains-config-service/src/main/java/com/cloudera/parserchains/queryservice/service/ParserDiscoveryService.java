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

package com.cloudera.parserchains.queryservice.service;

import com.cloudera.parserchains.core.model.define.ParserID;
import com.cloudera.parserchains.queryservice.model.describe.ParserDescriptor;
import com.cloudera.parserchains.queryservice.model.summary.ParserSummary;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * A service that finds and describes the parsers that are available.
 */
public interface ParserDiscoveryService {

  /**
   * Finds all of the parser types available for the user to build
   * parser chains with.
   */
  List<ParserSummary> findAll() throws IOException;

  /**
   * Describes the configuration parameters available for a given parser.
   * @param name The parser name.
   */
  ParserDescriptor describe(ParserID name) throws IOException;

  /**
   * Describes the configuration parameters for all available parser
   * types.
   */
  Map<ParserID, ParserDescriptor> describeAll() throws IOException;
}
