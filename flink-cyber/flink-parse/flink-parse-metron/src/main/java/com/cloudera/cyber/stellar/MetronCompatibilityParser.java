package com.cloudera.cyber.stellar;

import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.metron.common.Constants;
import org.apache.metron.common.configuration.FieldTransformer;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.utils.ReflectionUtils;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.apache.metron.parsers.interfaces.MessageParserResult;
import org.apache.metron.stellar.common.CachingStellarProcessor;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.apache.metron.stellar.dsl.functions.resolver.ClasspathFunctionResolver.Config.STELLAR_SEARCH_INCLUDES_KEY;

@Slf4j
@RequiredArgsConstructor
public class MetronCompatibilityParser {
    private final SensorParserConfig parserConfig;
    private final MessageParser<JSONObject> parser;
    private static final Map<String, Object> cacheConfig = ImmutableMap.of(
            CachingStellarProcessor.MAX_CACHE_SIZE_PARAM, 3000,
            CachingStellarProcessor.MAX_TIME_RETAIN_PARAM, 10,
            CachingStellarProcessor.RECORD_STATS, true
    );

    private static final Map<String, Object> stellarConfig = ImmutableMap.of(
            STELLAR_SEARCH_INCLUDES_KEY.param(), "org.apache.metron.*"
    );

    private final Context stellarContext;

    public static MetronCompatibilityParser of(InputStream configStream) throws IOException {
        SensorParserConfig parserConfig = SensorParserConfig.fromBytes(IOUtils.toByteArray(configStream));
        String parserClassName = parserConfig.getParserClassName();

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
                .with(Context.Capabilities.STELLAR_CONFIG, () -> stellarConfig )
                .build();
        StellarFunctions.FUNCTION_RESOLVER().initialize(stellarContext);
        return new MetronCompatibilityParser(parserConfig, parser, stellarContext);
    }

    public Optional<MessageParserResult<JSONObject>> parse(byte[] rawMessage) {
        Optional<MessageParserResult<JSONObject>> result = parser.parseOptionalResult(rawMessage);
        if (result.isPresent()) {
            // this parser can only return a single message - return the first message
            List<JSONObject> parsedMessages = result.get().getMessages();
            if (CollectionUtils.isNotEmpty(parsedMessages)) {
                JSONObject parsedMessage = parsedMessages.get(0);
                parsedMessage.putIfAbsent(Constants.Fields.ORIGINAL.getName(), new String(rawMessage));
                applyFieldTransformations(parsedMessage);
            }
        }
        return result;
    }

    private void applyFieldTransformations(JSONObject message) {
        for (FieldTransformer handler : parserConfig.getFieldTransformations()) {
            if (handler != null) {
                handler.transformAndUpdate(
                        message,
                        stellarContext,
                        parserConfig.getParserConfig()
                );
            }
        }
    }

}