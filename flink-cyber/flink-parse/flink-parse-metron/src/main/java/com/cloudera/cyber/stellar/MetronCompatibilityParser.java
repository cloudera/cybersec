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

package com.cloudera.cyber.stellar;

import com.cloudera.cyber.parser.MessageToParse;
import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.metron.common.configuration.FieldTransformer;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.message.metadata.RawMessage;
import org.apache.metron.common.utils.ReflectionUtils;
import org.apache.metron.parsers.DefaultMessageParserResult;
import org.apache.metron.parsers.filters.Filters;
import org.apache.metron.parsers.interfaces.MessageFilter;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.apache.metron.parsers.interfaces.MessageParserResult;
import org.apache.metron.stellar.common.CachingStellarProcessor;
import org.apache.metron.stellar.common.Constants;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.apache.metron.stellar.dsl.functions.resolver.ClasspathFunctionResolver.Config.STELLAR_SEARCH_INCLUDES_KEY;

@Slf4j
@RequiredArgsConstructor
public class MetronCompatibilityParser {
    private static final Map<String, Object> cacheConfig = ImmutableMap.of(
            CachingStellarProcessor.MAX_CACHE_SIZE_PARAM, 3000,
            CachingStellarProcessor.MAX_TIME_RETAIN_PARAM, 10,
            CachingStellarProcessor.RECORD_STATS, true
    );
    private static final Map<String, Object> stellarConfig = ImmutableMap.of(
            STELLAR_SEARCH_INCLUDES_KEY.param(), "org.apache.metron.*"
    );
    private final SensorParserConfig parserConfig;
    private final MessageFilter<JSONObject> filter;
    private final MessageParser<JSONObject> parser;
    private final Context stellarContext;
    private final String sensorType;

    public static MetronCompatibilityParser of(String sensorType, InputStream configStream) throws IOException {
        SensorParserConfig parserConfig = SensorParserConfig.fromBytes(IOUtils.toByteArray(configStream));
        String parserClassName = parserConfig.getParserClassName();

        MessageFilter<JSONObject> filter = null;
        if (!StringUtils.isEmpty(parserConfig.getFilterClassName())) {
            filter = Filters.get(
                    parserConfig.getFilterClassName(),
                    parserConfig.getParserConfig()
            );
        }
        log.info("Loading parser class {}", parserClassName);
        MessageParser<JSONObject> parser = ReflectionUtils.createInstance(parserClassName);
        log.info("Configuring Stellar parser with config {}", parserConfig.getParserConfig().toString());
        parser.configure(parserConfig.getParserConfig());
        log.info("Initializing parser");
        parser.init();
        log.info("Parser initialized");

        Cache<CachingStellarProcessor.Key, Object> cache = CachingStellarProcessor.createCache(cacheConfig);
        Context stellarContext = new Context.Builder()
                .with(Context.Capabilities.CACHE, () -> cache)
                .with(Context.Capabilities.STELLAR_CONFIG, () -> stellarConfig)
                .build();
        StellarFunctions.FUNCTION_RESOLVER().initialize(stellarContext);
        return new MetronCompatibilityParser(parserConfig, filter, parser, stellarContext, sensorType);
    }

    public Optional<MessageParserResult<JSONObject>> parse(MessageToParse messageToParse) {
        RawMessage metronRawMessage = MetronRawDataExtractor.INSTANCE.getRawMessage(parserConfig.getRawMessageStrategy(), messageToParse, parserConfig.getReadMetadata(),
                parserConfig.getRawMessageStrategyConfig());

        Optional<MessageParserResult<JSONObject>> result = parser.parseOptionalResult(metronRawMessage.getMessage());
        if (result.isPresent()) {
            // this parser can only return a single message - return the first message
            List<JSONObject> parsedMessages = result.get().getMessages();
            if (CollectionUtils.isNotEmpty(parsedMessages)) {
                JSONObject parsedMessage = parsedMessages.get(0);
                parserConfig.getRawMessageStrategy().mergeMetadata(
                        parsedMessage,
                        metronRawMessage.getMetadata(),
                        parserConfig.getMergeMetadata(),
                        parserConfig.getRawMessageStrategyConfig()
                );
                parsedMessage.put(Constants.SENSOR_TYPE, sensorType);

                parsedMessage.putIfAbsent(Constants.Fields.ORIGINAL.getName(),
                        new String(metronRawMessage.getMessage(), parser.getReadCharset()));
                applyFieldTransformations(parsedMessage, metronRawMessage);

                // return empty message - message should be filtered
                if (filter != null) {
                    boolean emit = filter.emit(parsedMessage, stellarContext);
                    if (!emit) {
                        return Optional.of(new DefaultMessageParserResult<>(Collections.emptyList()));
                    }
                }
            }
        }
        return result;
    }

    /**
     * Applies Stellar field transformations defined in the sensor parser config.
     *
     * @param message            Message parsed by the MessageParser
     * @param rawMessage         Raw message including metadata
     */
    private void applyFieldTransformations(JSONObject message, RawMessage rawMessage) {
        for (FieldTransformer handler : parserConfig.getFieldTransformations()) {
            if (handler != null) {
                if (!parserConfig.getMergeMetadata()) {
                    //if we haven't merged metadata, then we need to pass them along as configuration params.
                    handler.transformAndUpdate(
                            message,
                            stellarContext,
                            parserConfig.getParserConfig(),
                            rawMessage.getMetadata()
                    );
                } else {
                    handler.transformAndUpdate(
                            message,
                            stellarContext,
                            parserConfig.getParserConfig()
                    );
                }
            }
        }
    }

    }