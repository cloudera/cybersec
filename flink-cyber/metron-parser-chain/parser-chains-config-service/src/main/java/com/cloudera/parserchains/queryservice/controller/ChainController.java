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

import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.API_CHAINS;
import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.API_INDEXING;
import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.API_PARSER_TEST;
import com.cloudera.parserchains.core.model.define.ParserChainSchema;
import com.cloudera.parserchains.queryservice.model.describe.IndexMappingDescriptor;
import com.cloudera.parserchains.queryservice.model.exec.ChainTestRequest;
import com.cloudera.parserchains.queryservice.model.exec.ChainTestResponse;
import com.cloudera.parserchains.queryservice.model.summary.ParserChainSummary;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * The controller responsible for operations on parser chains.
 */
public interface ChainController {

  @ApiOperation(value = "Finds and returns all available parser chains.")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "A list of all parser chains.")
  })
  @GetMapping(value = API_CHAINS)
  ResponseEntity<List<ParserChainSummary>> findAll(
      @ApiParam(name = "pipelineName", value = "The pipeline to execute request in.")
      @RequestParam(name = "pipelineName", required = false) String pipelineName
  ) throws IOException;

  @ApiOperation(value = "Creates a new parser chain.")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Parser chain created successfully."),
      @ApiResponse(code = 404, message = "Unable to create a new parser chain.")
  })
  @PostMapping(value = API_CHAINS)
  ResponseEntity<ParserChainSchema> create(
      @ApiParam(name = "pipelineName", value = "The pipeline to execute request in.")
      @RequestParam(name = "pipelineName", required = false) String pipelineName,
      @ApiParam(name = "parserChain", value = "The parser chain to create.", required = true)
      @RequestBody ParserChainSchema chain) throws IOException;

  @ApiOperation(value = "Retrieves an existing parser chain.")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "The parser chain with the given ID."),
      @ApiResponse(code = 404, message = "The parser chain does not exist.")
  })
  @GetMapping(value = API_CHAINS + "/{id}")
  ResponseEntity<ParserChainSchema> read(
      @ApiParam(name = "pipelineName", value = "The pipeline to execute request in.")
      @RequestParam(name = "pipelineName", required = false) String pipelineName,
      @ApiParam(name = "id", value = "The ID of the parser chain to retrieve.", required = true)
      @PathVariable String id) throws IOException;

  @ApiOperation(value = "Updates an existing parser chain.")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = "The parser chain was updated."),
      @ApiResponse(code = 404, message = "The parser chain does not exist.")
  })
  @PutMapping(value = API_CHAINS + "/{id}")
  ResponseEntity<ParserChainSchema> update(
      @ApiParam(name = "pipelineName", value = "The pipeline to execute request in.")
      @RequestParam(name = "pipelineName", required = false) String pipelineName,
      @ApiParam(name = "parserChain", value = "The new parser chain definition.", required = true)
      @RequestBody ParserChainSchema chain,
      @ApiParam(name = "id", value = "The ID of the parser chain to update.")
      @PathVariable String id) throws IOException;

  @ApiOperation(value = "Deletes a parser chain.")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = "The parser chain was deleted."),
      @ApiResponse(code = 404, message = "The parser chain does not exist.")
  })
  @DeleteMapping(value = API_CHAINS + "/{id}")
  ResponseEntity<Void> delete(
      @ApiParam(name = "pipelineName", value = "The pipeline to execute request in.")
      @RequestParam(name = "pipelineName", required = false) String pipelineName,
      @ApiParam(name = "id", value = "The ID of the parser chain to delete.", required = true)
      @PathVariable String id) throws IOException;

  @ApiOperation(value = "Loads table mappings for the indexing job")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "The mapping file parsed successfully."),
  })
  @PostMapping(value = API_INDEXING)
  ResponseEntity<Map<String, Object>> getMappingsFromPath(
      @ApiParam(name = "pipelineName", value = "The pipeline to execute request in.")
      @RequestParam(name = "pipelineName", required = false) String pipelineName,
      @RequestBody IndexMappingDescriptor body) throws IOException;

  @ApiOperation(value = "Executes a parser chain to parse sample data.")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "The result of parsing the message."),
  })
  @PostMapping(value = API_PARSER_TEST)
  ResponseEntity<ChainTestResponse> test(
      @ApiParam(name = "testRun", value = "Describes the parser chain test to run.", required = true)
      @RequestBody ChainTestRequest testRun) throws IOException;

}
