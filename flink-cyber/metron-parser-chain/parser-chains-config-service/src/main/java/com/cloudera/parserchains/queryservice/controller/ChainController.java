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
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.core.fs.Path;
import org.springframework.http.ResponseEntity;
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

    @ApiOperation(value = "Finds and returns all available parser chains.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "A list of all parser chains.")
    })
    @GetMapping(value = API_CHAINS)
    public ResponseEntity<List<ParserChainSummary>> findAll(
            @ApiParam(name = "pipelineName", value = "The pipeline to execute request in.")
            @RequestParam(name = "pipelineName", required = false) String pipelineName
    ) throws IOException {
        String configPath = getConfigPath(pipelineName);

        List<ParserChainSummary> configs = chainPersistenceService.findAll(Paths.get(configPath));
        return ResponseEntity.ok(configs);
    }

    @ApiOperation(value = "Creates a new parser chain.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Parser chain created successfully."),
            @ApiResponse(code = 404, message = "Unable to create a new parser chain.")
    })
    @PostMapping(value = API_CHAINS)
    public ResponseEntity<ParserChainSchema> create(
            @ApiParam(name = "pipelineName", value = "The pipeline to execute request in.")
            @RequestParam(name = "pipelineName", required = false) String pipelineName,
            @ApiParam(name = "parserChain", value = "The parser chain to create.", required = true)
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

    @ApiOperation(value = "Retrieves an existing parser chain.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The parser chain with the given ID."),
            @ApiResponse(code = 404, message = "The parser chain does not exist.")
    })
    @GetMapping(value = API_CHAINS + "/{id}")
    public ResponseEntity<ParserChainSchema> read(
            @ApiParam(name = "pipelineName", value = "The pipeline to execute request in.")
            @RequestParam(name = "pipelineName", required = false) String pipelineName,
            @ApiParam(name = "id", value = "The ID of the parser chain to retrieve.", required = true)
            @PathVariable String id) throws IOException {
        String configPath = getConfigPath(pipelineName);

        ParserChainSchema chain = chainPersistenceService.read(id, Paths.get(configPath));
        if (null == chain) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(chain);
        }
    }

    @ApiOperation(value = "Updates an existing parser chain.")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "The parser chain was updated."),
            @ApiResponse(code = 404, message = "The parser chain does not exist.")
    })
    @PutMapping(value = API_CHAINS + "/{id}")
    public ResponseEntity<ParserChainSchema> update(
            @ApiParam(name = "pipelineName", value = "The pipeline to execute request in.")
            @RequestParam(name = "pipelineName", required = false) String pipelineName,
            @ApiParam(name = "parserChain", value = "The new parser chain definition.", required = true)
            @RequestBody ParserChainSchema chain,
            @ApiParam(name = "id", value = "The ID of the parser chain to update.")
            @PathVariable String id) throws IOException {
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

    @ApiOperation(value = "Deletes a parser chain.")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "The parser chain was deleted."),
            @ApiResponse(code = 404, message = "The parser chain does not exist.")
    })
    @DeleteMapping(value = API_CHAINS + "/{id}")
    public ResponseEntity<Void> delete(
            @ApiParam(name = "pipelineName", value = "The pipeline to execute request in.")
            @RequestParam(name = "pipelineName", required = false) String pipelineName,
            @ApiParam(name = "id", value = "The ID of the parser chain to delete.", required = true)
            @PathVariable String id) throws IOException {
        String configPath = getConfigPath(pipelineName);

        if (chainPersistenceService.delete(id, Paths.get(configPath))) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @ApiOperation(value = "Loads table mappings for the indexing job")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The mapping file parsed successfully."),
    })
    @PostMapping(value = API_INDEXING)
    public ResponseEntity<Map<String, Object>> getMappingsFromPath(
            @ApiParam(name = "pipelineName", value = "The pipeline to execute request in.")
            @RequestParam(name = "pipelineName", required = false) String pipelineName,
            @RequestBody IndexMappingDescriptor body) throws IOException {
        final String indexPath = getIndexingPath(body.getFilePath(), pipelineName);

        try {
            final Object mappingDtoMap = indexingService.getMappingsFromPath(indexPath);
            if (null == mappingDtoMap) {
                return ResponseEntity.notFound().build();
            } else {
                Map<String, Object> result = new HashMap<>();
                result.put("path", indexPath);
                result.put("result", mappingDtoMap);
                return ResponseEntity.ok(result);
            }
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to read mappings from the provided path");
        }
    }

    @ApiOperation(value = "Executes a parser chain to parse sample data.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The result of parsing the message."),
    })
    @PostMapping(value = API_PARSER_TEST)
    public ResponseEntity<ChainTestResponse> test(
            @ApiParam(name = "testRun", value = "Describes the parser chain test to run.", required = true)
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
