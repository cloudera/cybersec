package com.cloudera.cyber.indexing;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import javax.swing.*;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public abstract class SearchIndexJob {

    private static final long DEFAULT_RETRY_WINDOW = 5 * 60000;
    private static final String PARAMS_RETRY_WINDOW = "retry.window";

    protected final StreamExecutionEnvironment createPipeline(ParameterTool params) throws IOException {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        FlinkUtils.setupEnv(env, params);

        DataStream<Message> source = createSource(env, params);
        DataStream<Message> messages;

        // allow a general exclusion of any source that matches a regex pattern upfront to avoid retry loops
        if (StringUtils.isNotBlank(params.get("exclude.source.pattern",""))) {
            final Pattern filterExpression = Pattern.compile(params.get("exclude.source.pattern"));
            messages = source.filter(m -> !filterExpression.matcher(m.getSource()).matches());
        } else {
            messages = source;
        }

        // load the relevant index templates from search backend, and use to build field filters
        Map<String, Set<String>> fieldsBySource = loadFieldsFromIndex(params);
        KeyedStream<Map.Entry<String, Set<String>>, String> entryStringKeyedStream = env.fromCollection(fieldsBySource.entrySet()).keyBy(Map.Entry::getKey);
        final Set<String> indicesAvailable = fieldsBySource.keySet();

        // TODO - apply any row filtering logic here
        /*
         * Current plan is to pivot to table api, apply user configured where statement, if the statement is
         * applied before index entry transformation (better for performance; filter early to avoid extra work)
         * may also need to parse the filter clause and prepend map access parts, alternatively project the table
         * as a flat version of message, which may be more long term useful.
         */

        // only process messages that we have a collection for, and extract only the fields that are accounted for in target index
        DataStream<IndexEntry> output = messages
                .filter(message -> indicesAvailable.contains(message.getSource()))
                .keyBy(Message::getSource)
                .connect(entryStringKeyedStream)
                .process(new FilterStreamFieldsByConfig())
                .name("Index Entry Extractor").uid("index-entry-extract");

        DataStream<MessageRetry> unindexed = messages
                .filter(message -> !indicesAvailable.contains(message.getSource()))
                .map(m -> MessageRetry.builder()
                        .message(m)
                        // TODO - this should be set to the actual Flink processing time really
                        .lastTry(Instant.now().toEpochMilli())
                        .retry(0)
                        .build())
                .name("Messages not indexed").uid("warn-not-indexed");
        writeRetryResults(unindexed, params);

        // TODO work out retry logic with windowing to insert time delays
//        createRetrySource(env, params)
//                .assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor<MessageRetry>(Time.milliseconds(DEFAULT_RETRY_WINDOW)) {
//                    private static final long RETRY_INTERVAL = 60000;
//
//                    @Override
//                    public long extractTimestamp(MessageRetry messageRetry) {
//                        return messageRetry.getLastTry() + RETRY_INTERVAL;
//                    }
//                })
//                .keyBy(m -> m.getMessage().getSource())
//                .timeWindow(Time.milliseconds(params.getLong(PARAMS_RETRY_WINDOW, DEFAULT_RETRY_WINDOW)))
//                .allowedLateness(Time.milliseconds(DEFAULT_RETRY_WINDOW))
//                .process(new ProcessWindowFunction<MessageRetry, Message, String, TimeWindow>() {
//                    @Override
//                    public void process(String key, Context context, Iterable<MessageRetry> iterable, Collector<Message> collector) throws Exception {
//                        iterable.forEach(i -> collector.collect(i.getMessage()));
//                    }
//                }).name("Retry Window").uid("retry-window-process")
//
//                .keyBy(Message::getSource)
//                .connect(entryStringKeyedStream.keyBy(e -> e.getKey()))
//                .process(new FilterStreamFieldsByConfig())
//                .name("Retry processing").uid("index-entry-extract-retry");


        // now add the correctly formed version for tables

        // this will read an expected schema and map the incoming messages by type to the expected output fields
        // unknown fields will either be dumped in a 'custom' map field or dropped based on config

        writeResults(output, params);

        return env;
    }

    protected abstract Map<String, Set<String>> loadFieldsFromIndex(ParameterTool params) throws IOException;
    protected abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);
    protected abstract DataStream<MessageRetry> createRetrySource(StreamExecutionEnvironment env, ParameterTool params);
    protected abstract void writeResults(DataStream<IndexEntry> results, ParameterTool params) throws IOException;
    protected abstract void writeRetryResults(DataStream<MessageRetry> retries, ParameterTool params) throws IOException;

}
