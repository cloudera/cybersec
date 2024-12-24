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

package com.cloudera.cyber.indexing;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

@Slf4j
public abstract class SearchIndexJob {
    protected static final String PARAMS_SCHEMA_REFRESH_INTERVAL = "schema.refresh";
    protected static final long DEFAULT_SCHEMA_REFRESH_INTERVAL = 5 * 60 * 1000;
    protected static final String PARAMS_TOPIC_CONFIG_LOG = "topic.config.log";

    private static final long DEFAULT_RETRY_WINDOW = 5 * 60000;
    private static final String PARAMS_RETRY_WINDOW = "retry.window";
    private static final String PARAM_EXCLUDE_PATTERN = "exclude.source.pattern";

    protected final StreamExecutionEnvironment createPipeline(ParameterTool params) throws IOException {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        FlinkUtils.setupEnv(env, params);

        DataStream<Message> source = createSource(env, params);
        DataStream<Message> messages;

        // allow a general exclusion of any source that matches a regex pattern upfront to avoid retry loops
        if (StringUtils.isNotBlank(params.get(PARAM_EXCLUDE_PATTERN, ""))) {
            final Pattern filterExpression = Pattern.compile(params.get(PARAM_EXCLUDE_PATTERN));
            messages = source.filter(m -> !filterExpression.matcher(m.getSource()).matches());
        } else {
            messages = source;
        }

        DataStream<CollectionField> configSource = createConfigSource(env, params);
        logConfig(configSource, params);
        BroadcastStream<CollectionField> entryStringKeyedStream = configSource
              .broadcast(Descriptors.broadcastState);

        // TODO - apply any row filtering logic here
        // or at least you would be able to if that didn't rely on the broadcast state for config
        /*
         * Current plan is to pivot to table api, apply user configured where statement, if the statement is
         * applied before index entry transformation (better for performance; filter early to avoid extra work)
         * may also need to parse the filter clause and prepend map access parts, alternatively project the table
         * as a flat version of message, which may be more long term useful.
         */
        MapStateDescriptor<String, List<String>> broadcastState = new MapStateDescriptor<>(
              "fieldsByIndex",
              Types.STRING,
              Types.LIST(Types.STRING)
        );
        broadcastState.setQueryable("fieldsByIndex");

        // only process messages that we have a collection for, and extract only the fields that are accounted for in target index
        DataStream<IndexEntry> output = messages
              .keyBy(Message::getSource)
              .connect(entryStringKeyedStream)
              .process(new FilterStreamFieldsByConfig(broadcastState))
              .name("Index Entry Extractor").uid("index-entry-extract");

        // now add the correctly formed version for tables

        // this will read an expected schema and map the incoming messages by type to the expected output fields
        // unknown fields will either be dumped in a 'custom' map field or dropped based on config

        writeResults(output, params);

        return env;
    }


    protected static class Descriptors {
        public static final MapStateDescriptor<String, List<String>> broadcastState = new MapStateDescriptor<>(
              "fieldsByIndex",
              Types.STRING,
              Types.LIST(Types.STRING)
        );

    }

    protected abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);

    protected abstract DataStream<CollectionField> createConfigSource(StreamExecutionEnvironment env,
                                                                      ParameterTool params);

    protected abstract void writeResults(DataStream<IndexEntry> results, ParameterTool params) throws IOException;

    protected abstract void logConfig(DataStream<CollectionField> configSource, ParameterTool params);
}
