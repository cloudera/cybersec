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
import com.cloudera.cyber.parser.MessageToParse;
import com.cloudera.parserchains.core.utils.JSONUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.cloudera.cyber.parser.ParserJob.PARAM_PRIVATE_KEY;
import static com.cloudera.cyber.parser.ParserJob.PARAM_PRIVATE_KEY_FILE;

@Slf4j
public abstract class SplitJob {

    private static final String PARAMS_CONFIG_JSON = "config.json";
    private static final String PARAM_COUNT_INTERVAL = "count.interval";
    private static final long DEFAULT_COUNT_INTERVAL = 5 * 60 * 1000;
    protected String configJson;

    protected StreamExecutionEnvironment createPipeline(ParameterTool params) throws Exception {
        List<SplitConfig> configs = parseConfig();
        Map<String, SplitConfig> configMap = configs.stream().collect(Collectors.toMap(k -> k.getTopic(), v -> v));

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        DataStream<MessageToParse> source = createSource(env, params, configMap.keySet());

        BroadcastStream<SplitConfig> configStream = createConfigSource(env, params)
                .broadcast(Descriptors.broadcastState);

        byte[] privKeyBytes = params.has(PARAM_PRIVATE_KEY_FILE) ?
                Files.readAllBytes(Paths.get(params.get(PARAM_PRIVATE_KEY_FILE))) :
                Base64.getDecoder().decode(params.getRequired(PARAM_PRIVATE_KEY));

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(privKeyBytes);
        PrivateKey signKey = keyFactory.generatePrivate(privSpec);

        SingleOutputStreamOperator<Message> results = source
                .keyBy(k -> k.getTopic())
                .connect(configStream)
                .process(new SplitBroadcastProcessFunction(configMap, signKey));

        writeOriginalsResults(params, source);

        SingleOutputStreamOperator<Message> parsed = results.map(new ParserChainMapFunction(configMap));


        DataStream<Tuple2<String, Long>> counts = parsed.map(new MapFunction<Message, Tuple2<String, Long>>() {
            @Override
            public Tuple2<String, Long> map(Message message) throws Exception {
                return Tuple2.of(message.getSource(), 1L);
            }
        })
                .keyBy(0)
                .timeWindow(Time.milliseconds(params.getLong(PARAM_COUNT_INTERVAL, DEFAULT_COUNT_INTERVAL)))
                .allowedLateness(Time.milliseconds(0))
                .sum(1);
        writeCounts(params, counts);


        writeResults(params, parsed);

        return env;
    }

    protected static class Descriptors {
        public static MapStateDescriptor<String, SplitConfig> broadcastState = new MapStateDescriptor<String, SplitConfig>("configs", String.class, SplitConfig.class);
    }

    protected List<SplitConfig> parseConfig() throws IllegalArgumentException, IOException {
        try {
            return JSONUtils.INSTANCE.getMapper().readValue(configJson, new TypeReference<List<SplitConfig>>() {
            });
        } catch (Exception e) {
            log.error(String.format("Failed to read split config %s", configJson));
            throw new IllegalArgumentException("Config could not be read for splits", e);
        }
    }

    protected abstract DataStream<SplitConfig> createConfigSource(StreamExecutionEnvironment env, ParameterTool params);

    protected abstract void writeResults(ParameterTool params, DataStream<Message> results);

    protected abstract void writeOriginalsResults(ParameterTool params, DataStream<MessageToParse> results);

    protected abstract void writeCounts(ParameterTool params, DataStream<Tuple2<String, Long>> sums);

    protected abstract DataStream<MessageToParse> createSource(StreamExecutionEnvironment env, ParameterTool params, Iterable<String> topics);
}
