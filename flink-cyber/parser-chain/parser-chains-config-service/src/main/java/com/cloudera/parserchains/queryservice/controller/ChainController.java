package com.cloudera.parserchains.queryservice.controller;

import com.cloudera.parserchains.core.ChainLink;
import com.cloudera.parserchains.core.InvalidParserException;
import com.cloudera.parserchains.core.model.define.ParserChainSchema;
import com.cloudera.parserchains.queryservice.config.AppProperties;
import com.cloudera.parserchains.queryservice.model.exec.ChainTestRequest;
import com.cloudera.parserchains.queryservice.model.exec.ChainTestResponse;
import com.cloudera.parserchains.queryservice.model.exec.ParserResult;
import com.cloudera.parserchains.queryservice.model.exec.ResultLog;
import com.cloudera.parserchains.queryservice.model.summary.ParserChainSummary;
import com.cloudera.parserchains.queryservice.service.ChainBuilderService;
import com.cloudera.parserchains.queryservice.service.ChainExecutorService;
import com.cloudera.parserchains.queryservice.service.ChainPersistenceService;
import com.cloudera.parserchains.queryservice.service.ResultLogBuilder;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;

import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.*;

/**
 * The controller responsible for operations on parser chains.
 */
@RestController
@RequestMapping(value = PARSER_CONFIG_BASE_URL)
public class ChainController {
    private static final Logger logger = LogManager.getLogger(ChainController.class);

    /**
     * The maximum number of sample text values that can be used to test a parser chain.
     */
    static final int MAX_SAMPLES_PER_TEST = 200;

    @Autowired
    ChainPersistenceService chainPersistenceService;

    @Autowired
    ChainBuilderService chainBuilderService;

    @Autowired
    ChainExecutorService chainExecutorService;

    @Autowired
    AppProperties appProperties;

    @ApiOperation(value = "Finds and returns all available parser chains.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "A list of all parser chains.")
    })
    @GetMapping(value = API_CHAINS)
    ResponseEntity<List<ParserChainSummary>> findAll() throws IOException {
        String configPath = appProperties.getConfigPath();
        List<ParserChainSummary> configs = chainPersistenceService.findAll(Paths.get(configPath));
        return ResponseEntity.ok(configs);
    }

    @ApiOperation(value = "Creates a new parser chain.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Parser chain created successfully."),
            @ApiResponse(code = 404, message = "Unable to create a new parser chain.")
    })
    @PostMapping(value = API_CHAINS)
    ResponseEntity<ParserChainSchema> create(
            @ApiParam(name = "parserChain", value = "The parser chain to create.", required = true)
            @RequestBody ParserChainSchema chain) throws IOException {
        String configPath = appProperties.getConfigPath();
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
    ResponseEntity<ParserChainSchema> read(
            @ApiParam(name = "id", value = "The ID of the parser chain to retrieve.", required = true)
            @PathVariable String id) throws IOException {
        String configPath = appProperties.getConfigPath();
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
    ResponseEntity<ParserChainSchema> update(
            @ApiParam(name = "parserChain", value = "The new parser chain definition.", required = true)
            @RequestBody ParserChainSchema chain,
            @ApiParam(name = "id", value = "The ID of the parser chain to update.")
            @PathVariable String id) {
        String configPath = appProperties.getConfigPath();
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
    ResponseEntity<Void> delete(
            @ApiParam(name = "id", value = "The ID of the parser chain to delete.", required = true)
            @PathVariable String id) throws IOException {
        String configPath = appProperties.getConfigPath();
        if (chainPersistenceService.delete(id, Paths.get(configPath))) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @ApiOperation(value = "Executes a parser chain to parse sample data.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The result of parsing the message."),
    })
    @PostMapping(value = API_PARSER_TEST)
    ResponseEntity<ChainTestResponse> test(
            @ApiParam(name = "testRun", value = "Describes the parser chain test to run.", required = true)
            @RequestBody ChainTestRequest testRun) {
        ParserChainSchema chain = testRun.getParserChainSchema();
        ChainTestResponse results = new ChainTestResponse();
        testRun.getSampleData().getSource()
                .stream()
                .limit(MAX_SAMPLES_PER_TEST)
                .map(sample -> doTest(chain, sample))
                .forEach(result -> results.addResult(result));
        return ResponseEntity.ok(results);
    }

    /**
     * Parse sample text using a parser chain.
     * @param schema Defines the parser chain that needs to be constructed.
     * @param textToParse The text to parse.
     * @return
     */
    private ParserResult doTest(ParserChainSchema schema, String textToParse) {
        ParserResult result;
        try {
            ChainLink chain = chainBuilderService.build(schema);
            result = chainExecutorService.execute(chain, textToParse);

        } catch(InvalidParserException e) {
            logger.info("The parser chain is invalid as constructed.", e);
            ResultLog log = ResultLogBuilder.error()
                    .parserId(e.getBadParser().getLabel())
                    .exception(e)
                    .build();
            result = new ParserResult()
                    .setLog(log);
        }
        return result;
    }
}
