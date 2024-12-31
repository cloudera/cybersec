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

package com.cloudera.cyber.caracal;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.SignedSourceKey;
import com.cloudera.cyber.parser.MessageToParse;
import com.cloudera.cyber.rules.RuleType;
import com.cloudera.cyber.rules.engines.RuleEngine;
import com.google.common.collect.Streams;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;
import org.apache.flink.util.Preconditions;

import javax.script.ScriptException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;

/**
 * Splits up JSON array based on a configured path and dupes 'header' in to multiple messages
 */
@Slf4j
public class SplittingFlatMapFunction extends RichFlatMapFunction<MessageToParse, Message> {
    @NonNull
    private final SplitConfig config;
    @NonNull
    private final PrivateKey signKey;

    private transient String topic;
    private transient JsonPath headerPath;
    private transient String tsField;
    private transient TimestampSource timestampSource;

    private transient MapFunction<String, Long> timestampFunction;
    private transient RuleEngine engine;

    private transient Signature signature;

    public enum TimestampSource {
        HEADER, SPLIT
    }
    private JsonPath jsonPath;

    private static final com.jayway.jsonpath.Configuration STRICT_PROVIDER_CONFIGURATION =
            com.jayway.jsonpath.Configuration.builder().jsonProvider(new JacksonJsonProvider()).build();

    public SplittingFlatMapFunction(SplitConfig config, PrivateKey signKey) {
        Preconditions.checkNotNull(signKey, "Must supply signing key");
        Preconditions.checkNotNull(config, "Must supply split config");

        this.config = config;
        this.signKey = signKey;
    }

    private void setTimestampFunction(String timestampFunction) {
        String fn = UUID.randomUUID().toString().replace("-", "");
        try {
            this.engine.eval("function transform" + fn + "(ts) { return " + timestampFunction + "}");
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
        this.timestampFunction = (String s) -> {
            Double out = (Double) engine.invokeFunction("transform" + fn, s);
            return out.longValue();
        };
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);

        this.topic = config.getTopic();
        this.jsonPath = JsonPath.compile(config.getSplitPath());
        this.headerPath = JsonPath.compile(config.getHeaderPath());
        this.tsField = config.getTimestampField();
        this.timestampSource = config.getTimestampSource();

        try {
            this.signature = Signature.getInstance("SHA1WithRSA");
            this.signature.initSign(signKey);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to initialise signing", e);
        }

        if(config.getTimestampFunction() != null & !config.getTimestampFunction().isEmpty()) {
            this.engine = RuleType.JS.engine("");

            this.setTimestampFunction(config.getTimestampFunction());
        } else {
            this.engine = null;
            this.timestampFunction = (String s) -> {
                return Long.valueOf(s);
            };
        }
    }

    @Override
    public void flatMap(MessageToParse input, Collector<Message> collector) throws Exception {
        // sign the source content
        signature.update(input.getOriginalBytes());
        final byte[] sig = signature.sign();

        DocumentContext documentContext = JsonPath.using(STRICT_PROVIDER_CONFIGURATION).parse(new String(input.getOriginalBytes(), StandardCharsets.UTF_8));

        // header is all top level simple type fields.
        Object header = documentContext.read(headerPath);
        new HashMap<>();
        Map<String, Object> headerFields = header instanceof Map ?
                ((Map<String, Object>) header).entrySet().stream()
                    .filter(e -> isScalar(e.getValue()))
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)) :
                emptyMap();

        Object jsonPathResult = documentContext.read(jsonPath);

        ((List) jsonPathResult).forEach(part -> {
            Map<String, Object> fields = (Map<String, Object>) part;

            String tsString = (String) (timestampSource == TimestampSource.SPLIT ?
                                fields.get(tsField) :
                                headerFields.get(tsField));
            long ts;
            try {
                ts = Long.valueOf(tsString);
            } catch (NumberFormatException e) {
                try {
                    ts = this.timestampFunction.map(tsString);
                } catch (Exception ex) {
                    throw new RuntimeException("Timestamp script failed", ex);
                }
            }

            // Add metrics
            /*MetricGroup metricGroup = getRuntimeContext().getMetricGroup();
            metricGroup.addGroup("parser." + input.getTopic()).counter(1);*/

            collector.collect(Message.builder()
                    .originalSource(
                            SignedSourceKey.builder()
                                    .topic(input.getTopic())
                                    .partition(input.getPartition())
                                    .offset(input.getOffset())
                                    .signature(sig)
                                    .build()
                    )
                    .source(input.getTopic())
                    .ts(ts)
                    .extensions(childPlusHeader(fields, headerFields)).build());
        });
        
    }

    private boolean isScalar(Object value) {
        return !(value instanceof Map || value instanceof List);
    }

    private Map<String, String> childPlusHeader(Map<String, Object> part, Map<String, Object> header) {
        Map<String, String> result = Streams.concat(header.entrySet().stream(), part.entrySet().stream())
                .collect(toMap(Map.Entry::getKey, v -> v.getValue().toString(), (a, b) -> b));
        return result;
    }

}
