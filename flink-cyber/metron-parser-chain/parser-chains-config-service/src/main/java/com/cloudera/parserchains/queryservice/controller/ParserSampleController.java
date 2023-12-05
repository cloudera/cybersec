/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloudera.parserchains.queryservice.controller;

import com.cloudera.parserchains.queryservice.config.AppProperties;
import com.cloudera.parserchains.queryservice.model.describe.SampleFolderDescriptor;
import com.cloudera.parserchains.queryservice.model.sample.ParserSample;
import com.cloudera.parserchains.queryservice.service.ParserSampleService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.API_PARSER_TEST_SAMPLES;
import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.PARSER_CONFIG_BASE_URL;

/**
 * The controller responsible for operations on parsers.
 */
@RestController
@RequestMapping(value = PARSER_CONFIG_BASE_URL)
public class ParserSampleController {

    @Autowired
    private ParserSampleService parserSampleService;

    @Autowired
    private AppProperties appProperties;

    @ApiOperation(value = "Retrieves all parser samples for the specified chain.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "A list of all parser samples for the specified chain.")
    })
    @PostMapping(value = API_PARSER_TEST_SAMPLES + "/{id}")
    @PreAuthorize("@spnegoUserDetailsService.hasAccess('get', '*')")
    ResponseEntity<List<ParserSample>> findAllById(@ApiParam(name = "id", value = "The ID of the parser chain to retrieve samples for.", required = true)
                                                   @PathVariable String id,
                                                   @RequestBody SampleFolderDescriptor body) throws IOException {
        String sampleFolderPath = getSampleFolderPath(body);
        List<ParserSample> types = parserSampleService.findAllById(sampleFolderPath, id);
        if (types == null){
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(types);
    }

    @ApiOperation(value = "Create or replace parser chain sample list.")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "The parser chain list was created/replaced."),
            @ApiResponse(code = 404, message = "The parser chain does not exist.")
    })
    @PutMapping(value = API_PARSER_TEST_SAMPLES + "/{id}")
    @PreAuthorize("@spnegoUserDetailsService.hasAccess('put', '*')")
    ResponseEntity<List<ParserSample>> update(
            @ApiParam(name = "sampleList", value = "The new sample definition list.", required = true)
            @RequestBody SampleFolderDescriptor body,
            @ApiParam(name = "id", value = "The ID of the parser chain sample to update.")
            @PathVariable String id) {
        String sampleFolderPath = getSampleFolderPath(body);
        try {
            List<ParserSample> createdSampleList = parserSampleService.update(sampleFolderPath, id, body.getSampleList());
            if (null == createdSampleList) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity
                    .created(URI.create(API_PARSER_TEST_SAMPLES + "/" + id))
                    .body(createdSampleList);
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to create parser chain samples with id=" + id);
        }
    }

    private String getSampleFolderPath(SampleFolderDescriptor body) {
        String sampleFolderPath = StringUtils.hasText(body.getFolderPath())
                ? body.getFolderPath()
                : appProperties.getSampleFolderPath();
        if (!sampleFolderPath.endsWith("/")){
            sampleFolderPath = sampleFolderPath.concat("/");
        }
        return sampleFolderPath;
    }

}
