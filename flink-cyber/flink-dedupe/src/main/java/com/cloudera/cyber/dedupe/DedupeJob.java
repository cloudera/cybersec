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

package com.cloudera.cyber.dedupe;

import static com.cloudera.cyber.dedupe.Dedupe.dedupe;

import com.cloudera.cyber.DedupeMessage;
import com.cloudera.cyber.Message;
import java.util.Arrays;
import java.util.List;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.OutputTag;

/**
 * Deduplication Job
 *
 * <p>
 * Provide a set of fields to group on, a limit for the time window and the count of duplicates
 * before emitting, and receive a message with the values of the deduped fields, a count and the
 * maximum and minimum timestamp for the message rolled into the duplicate.
 * 
 * <p>
 * This will need to be output to a separate kafka topic for each de-dupe
 *
 * <p>
 * TODO - maybe. Add a salt to the key for keys that are likely to be high duplicate
 * this will avoid all the messages from the same key being shunted to one task
 * Note that to do this really smartly, we could auto watch the count metrics and
 * use state to readjust for very heavy keys based on historical behavior, this would
 * provide some self-healing and self-tuning
 */
public abstract class DedupeJob {
    protected static final String PARAM_DEDUPE_KEY = "dedupe.keys";
    protected static final String PARAM_DEDUPE_MAX_TIME = "dedupe.limit.time";
    protected static final String PARAM_DEDUPE_MAX_COUNT = "dedupe.limit.count";
    protected static final String PARAM_DEDUPE_LATENESS = "dedupe.lateness";
    private static final long DEFAULT_SESSION_TIME = 1000;

    protected StreamExecutionEnvironment createPipeline(ParameterTool params) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        List<String> key = Arrays.asList(params.get(PARAM_DEDUPE_KEY).split(","));
        Long maxTime = params.getLong(PARAM_DEDUPE_MAX_TIME, DEFAULT_SESSION_TIME);
        Long maxCount = params.getLong(PARAM_DEDUPE_MAX_COUNT, 0);

        DataStream<Message> source = createSource(env, params, key, maxTime);
        final OutputTag<DedupeMessage> lateData = new OutputTag<DedupeMessage>("late-data") {
        };
        Time allowedLateness = Time.milliseconds(params.getLong(PARAM_DEDUPE_LATENESS, 0L));
        SingleOutputStreamOperator<DedupeMessage> results =
              dedupe(source, key, maxTime, maxCount, lateData, allowedLateness);
        writeResults(params, results);
        //printResults(results);

        // capture and publish any late results without counts, i.e. fail safe
        writeResults(params, results.getSideOutput(lateData).map(d -> d.toBuilder().late(true).build()));

        return env;
    }

    private void printResults(SingleOutputStreamOperator<DedupeMessage> results) {
        results.print();
    }

    protected abstract void writeResults(ParameterTool params, DataStream<DedupeMessage> results);

    protected abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params,
                                                        List<String> sessionKey, Long sessionTimeout);
}
