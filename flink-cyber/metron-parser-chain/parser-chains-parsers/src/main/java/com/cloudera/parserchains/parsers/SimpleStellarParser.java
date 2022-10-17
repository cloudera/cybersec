package com.cloudera.parserchains.parsers;

import com.cloudera.parserchains.core.Message;
import com.cloudera.parserchains.core.Parser;
import com.cloudera.parserchains.core.catalog.Configurable;
import com.cloudera.parserchains.core.catalog.MessageParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.flink.core.fs.FSDataInputStream;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;
import org.apache.metron.common.configuration.enrichment.handler.ConfigHandler;
import org.apache.metron.enrichment.adapters.stellar.StellarAdapter;
import org.apache.metron.stellar.common.StellarProcessor;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.MapVariableResolver;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@MessageParser(
        name = "Metron Stellar parser",
        description = "Metron compatibility parser.")
@Slf4j
public class SimpleStellarParser implements Parser {

    private ConfigHandler configHandler;
    private StellarProcessor processor;
    private Context stellarContext;


    public SimpleStellarParser() {
        updateExpressionList(Collections.emptyList());
        this.processor = new StellarProcessor();
        this.stellarContext = Context.EMPTY_CONTEXT();
    }

    @Configurable(
            key = "stellarPath",
            label = "Stellar File Path",
            description = "Path to stellar file")
    public SimpleStellarParser stellarPath(String pathToStellar) throws IOException {
        FileSystem fileSystem = new Path(pathToStellar).getFileSystem();
        loadExpressions(pathToStellar, fileSystem);
        return this;
    }

    private void loadExpressions(String pathToConfig, FileSystem fileSystem) throws IOException {
        Path configPath = new Path(pathToConfig);
        try (FSDataInputStream fsDataInputStream = fileSystem.open(configPath)) {
            final String fileContents = IOUtils.toString(fsDataInputStream, StandardCharsets.UTF_8);
            final List<String> expressionList = Arrays.asList(fileContents.split("\n"));

            updateExpressionList(expressionList);
        } catch (Exception e) {
            log.error(String.format("Could not create simple stellar expressions from '%s'", pathToConfig), e);
            throw e;
        }
    }

    private void updateExpressionList(List<String> expressionList) {
        final HashMap<String, Object> configMap = new HashMap<>();
        configMap.put("config", expressionList);
        configMap.put("type", "STELLAR");
        this.configHandler = new ConfigHandler(null, configMap);
    }

    @Override
    public Message parse(Message input) {
        Message.Builder builder = Message.builder().withFields(input);
        final Map<String, Object> fieldMap = input.getFields().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().get(), e -> e.getValue().get()));
        return doParse(fieldMap, builder);
    }

    private Message doParse(Map<String, Object> toParse, Message.Builder output) {
        final MapVariableResolver resolver = new MapVariableResolver(toParse);
        try {
            final JSONObject result = StellarAdapter.process(toParse, configHandler, "", 1000L, processor, resolver, stellarContext);
            result.forEach((key, value) -> output.addField(String.valueOf(key), String.valueOf(value)));
        } catch (Exception e) {
            output.withError("Parser did not return a message result").build();
        }
        return output.build();
    }

}