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

import com.cloudera.parserchains.core.ChainLink;
import com.cloudera.parserchains.core.InvalidParserException;
import com.cloudera.parserchains.core.model.define.ParserChainSchema;
import com.cloudera.parserchains.queryservice.config.AppProperties;
import com.cloudera.parserchains.queryservice.model.describe.IndexMappingDescriptor;
import com.cloudera.parserchains.queryservice.model.exec.ChainTestRequest;
import com.cloudera.parserchains.queryservice.model.exec.ChainTestResponse;
import com.cloudera.parserchains.queryservice.model.exec.ParserResult;
import com.cloudera.parserchains.queryservice.model.exec.PipelineResult;
import com.cloudera.parserchains.queryservice.model.exec.ResultLog;
import com.cloudera.parserchains.queryservice.model.summary.ParserChainSummary;
import com.cloudera.parserchains.queryservice.service.ChainBuilderService;
import com.cloudera.parserchains.queryservice.service.ChainExecutorService;
import com.cloudera.parserchains.queryservice.service.ChainPersistenceService;
import com.cloudera.parserchains.queryservice.service.IndexingService;
import com.cloudera.parserchains.queryservice.service.PipelineService;
import com.cloudera.parserchains.queryservice.service.ResultLogBuilder;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.core.fs.Path;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.API_CHAINS;
import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.API_CHAINS_READ_URL;
import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.API_INDEXING;
import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.API_PARSER_TEST;
import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.PARSER_CONFIG_BASE_URL;

/**
 * The controller responsible for operations on parser chains.
 */
@Slf4j
@RestController
@RequestMapping(value = PARSER_CONFIG_BASE_URL)
@RequiredArgsConstructor
public class ChainController {

    /**
     * The maximum number of sample text values that can be used to test a parser chain.
     */
    static final int MAX_SAMPLES_PER_TEST = 200;

    private final ChainPersistenceService chainPersistenceService;

    private final ChainBuilderService chainBuilderService;

    private final ChainExecutorService chainExecutorService;

    private final PipelineService pipelineService;

    private final IndexingService indexingService;

    private final AppProperties appProperties;


    @Operation(summary = "Retrieves all available parser chains.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "A list of all parser chains.", content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ParserChainSummary.class))))
            })
    @GetMapping(value = API_CHAINS)
    @PreAuthorize("@spnegoUserDetailsService.hasAccess('get', #pipelineName)")
    public ResponseEntity<List<ParserChainSummary>> findAll(
            @ApiParam(name = "pipelineName", value = "The pipeline to execute request in.")
            @RequestParam(name = "pipelineName", required = false) String pipelineName
    ) throws IOException {
        String configPath = getConfigPath(pipelineName);

        List<ParserChainSummary> configs = chainPersistenceService.findAll(Paths.get(configPath));
        return ResponseEntity.ok(configs);
    }

    @Operation(summary = "Creates a new parser chain.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The parser chain was created.", content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ParserChainSchema.class))),
                    @ApiResponse(responseCode = "404", description = "Unable to create a new parser chain.")
            })
    @PostMapping(value = API_CHAINS)
    @PreAuthorize("@spnegoUserDetailsService.hasAccess('post', #pipelineName)")
    public ResponseEntity<ParserChainSchema> create(
            @ApiParam(name = "pipelineName", value = "The pipeline to execute request in.")
            @RequestParam(name = "pipelineName", required = false) String pipelineName,
            @Parameter(name = "parserChain", description = "The parser chain to create.", required = true)
            @RequestBody ParserChainSchema chain) throws IOException {
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


    @Operation(summary = "Retrieves an existing parser chain.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The parser chain with the given ID.", content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ParserChainSchema.class))),
                    @ApiResponse(responseCode = "404", description = "The parser chain does not exist.")
            })
    @GetMapping(value = API_CHAINS + "/{id}")
    @PreAuthorize("@spnegoUserDetailsService.hasAccess('get', #pipelineName)")
    public ResponseEntity<ParserChainSchema> read(
            @ApiParam(name = "pipelineName", value = "The pipeline to execute request in.")
            @RequestParam(name = "pipelineName", required = false) String pipelineName,
            @Parameter(name = "id", description = "The ID of the parser chain to retrieve.", required = true)
            @PathVariable String id) throws IOException {
        String configPath = getConfigPath(pipelineName);

        ParserChainSchema chain = chainPersistenceService.read(id, Paths.get(configPath));
        if (null == chain) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(chain);
        }
    }


    @Operation(summary = "Updates an existing parser chain.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "The parser chain was updated."),
                    @ApiResponse(responseCode = "404", description = "The parser chain does not exist.")
            })
    @PutMapping(value = API_CHAINS + "/{id}")
    @PreAuthorize("@spnegoUserDetailsService.hasAccess('put', #pipelineName)")
    public ResponseEntity<ParserChainSchema> update(
            @ApiParam(name = "pipelineName", value = "The pipeline to execute request in.")
            @RequestParam(name = "pipelineName", required = false) String pipelineName,
            @Parameter(name = "parserChain", description = "The new parser chain definition.", required = true)
            @RequestBody ParserChainSchema chain,
            @Parameter(name = "id", description = "The ID of the parser chain to update.")
            @PathVariable String id) throws IOException {
        String configPath = getConfigPath(pipelineName);
        try {
            ParserChainSchema updatedChain = chainPersistenceService.update(id, chain, Paths.get(configPath));
            if (updatedChain == null) {
                return ResponseEntity.notFound().build();
            } else {
                return ResponseEntity.noContent().build();
            }
            // TODO: fix exception handling
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to update configuration with id=" + id);
        }
    }

    @Operation(summary = "Deletes a parser chain."
            , responses = {
            @ApiResponse(responseCode = "204", description = "The parser chain was deleted."),
            @ApiResponse(responseCode = "404", description = "The parser chain does not exist.")
    })
    @DeleteMapping(value = API_CHAINS + "/{id}")
    @PreAuthorize("@spnegoUserDetailsService.hasAccess('delete', #pipelineName)")
    public ResponseEntity<Void> delete(
            @ApiParam(name = "pipelineName", value = "The pipeline to execute request in.")
            @RequestParam(name = "pipelineName", required = false) String pipelineName,
            @Parameter(name = "id", description = "The ID of the parser chain to delete.", required = true)
            @PathVariable String id) throws IOException {
        String configPath = getConfigPath(pipelineName);

        if (chainPersistenceService.delete(id, Paths.get(configPath))) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }


    @Operation(summary = "Loads table mappings for the indexing job.",
        responses = {
                    @ApiResponse(responseCode = "200", description = "The mapping file parsed successfully."),
            })
    @PostMapping(value = API_INDEXING)
    @PreAuthorize("@spnegoUserDetailsService.hasAccess('get', #pipelineName)")
    public ResponseEntity<Map<String, Object>> getMappingsFromPath(
            @ApiParam(name = "pipelineName", value = "The pipeline to execute request in.")
            @RequestParam(name = "pipelineName", required = false) String pipelineName,
            @RequestBody IndexMappingDescriptor body) throws IOException {
        final String indexPath = getIndexingPath(body.getFilePath(), pipelineName);

        try {
            final Object mappingDtoMap = indexingService.getMappingsFromPath(indexPath);
            if (null == mappingDtoMap) {
                return ResponseEntity.noContent().build();
            } else {
                Map<String, Object> result = new HashMap<>();
                result.put("path", indexPath);
                result.put("result", mappingDtoMap);
                return ResponseEntity.ok(result);
            }
            // TODO: fix exception handling
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to read mappings from the provided path");
        }
    }


    @Operation(summary = "Executes a parser chain to parse sample data.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The result of parsing the message.", content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ChainTestResponse.class)))
            })
    @PostMapping(value = API_PARSER_TEST)
    @PreAuthorize("@spnegoUserDetailsService.hasAccess('get', '*')")
    public ResponseEntity<ChainTestResponse> test(
            @Parameter(name = "testRun", description = "Describes the parser chain test to run.", required = true)
            @RequestBody ChainTestRequest testRun) throws IOException {
        ParserChainSchema chain = testRun.getParserChainSchema();
        ChainTestResponse results = new ChainTestResponse();
        testRun.getSampleData().getSource()
                .stream()
                .limit(MAX_SAMPLES_PER_TEST)
                .map(sample -> doTest(chain, sample))
                .forEach(results::addResult);
        return ResponseEntity.ok(results);
    }

    /**
     * Parse sample text using a parser chain.
     *
     * @param schema      Defines the parser chain that needs to be constructed.
     * @param textToParse The text to parse.
     * @return The result of parsing the text.
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
        return getPipelinePath(pipelineName, appProperties::getConfigPath);
    }

    private String getIndexingPath(String filePath, String pipelineName) throws IOException {
        if (StringUtils.hasText(filePath)) {
            return filePath;
        }
        return getPipelinePath(pipelineName, appProperties::getIndexPath);
    }

    private String getPipelinePath(String pipelineName, Supplier<String> defaultPathSupplier) throws IOException {
        if (!StringUtils.hasText(pipelineName)) {
            return defaultPathSupplier.get();
        }

        return Optional.ofNullable(pipelineService.findAll())
                .map(pipelineMap -> pipelineMap.get(pipelineName))
                .map(PipelineResult::getPath)
                .map(Path::getPath)
                .orElseGet(defaultPathSupplier);
    }
}
