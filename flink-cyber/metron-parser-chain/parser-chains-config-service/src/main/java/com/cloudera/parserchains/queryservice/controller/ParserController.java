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

package com.cloudera.parserchains.queryservice.controller;

import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.API_PARSER_FORM_CONFIG;
import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.API_PARSER_TYPES;
import com.cloudera.parserchains.core.model.define.ParserID;
import com.cloudera.parserchains.queryservice.model.describe.ParserDescriptor;
import com.cloudera.parserchains.queryservice.model.summary.ParserSummary;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * The controller responsible for operations on parsers.
 */
public interface ParserController {

  @ApiOperation(value = "Retrieves all available parsers.")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "A list of all parser types.")
  })
  @GetMapping(value = API_PARSER_TYPES)
  ResponseEntity<List<ParserSummary>> findAll() throws IOException;

  @ApiOperation(value = "Describes the configuration parameters for all available parsers.")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "A map of parser types and their associated configuration parameters."),
      @ApiResponse(code = 404, message = "Unable to retrieve.")
  })
  @GetMapping(value = API_PARSER_FORM_CONFIG)
  ResponseEntity<Map<ParserID, ParserDescriptor>> describeAll() throws IOException;

}
