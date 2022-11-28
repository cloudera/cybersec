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

package com.cloudera.cyber.sessions;

import com.cloudera.cyber.GroupedMessage;
import com.cloudera.cyber.Message;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.Arrays;
import java.util.List;

import static com.cloudera.cyber.sessions.Session.sessionize;

public abstract class SessionJob {
    private static final long DEFAULT_SESSION_TIMEOUT = 1000;
    protected static final String PARAM_SESSION_KEY = "session.key";
    protected static final String PARAM_SESSION_TIMEOUT = "session.timeout";
    protected static final String PARAM_SESSION_LIMIT = "session.limit";

    public StreamExecutionEnvironment createPipeline(ParameterTool params) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        List<String> sessionKey = Arrays.asList(params.get(PARAM_SESSION_KEY).split(","));
        Long sessionTimeout = params.getLong(PARAM_SESSION_TIMEOUT, DEFAULT_SESSION_TIMEOUT);

        DataStream<Message> source = createSource(env, params, sessionKey, sessionTimeout);
        SingleOutputStreamOperator<GroupedMessage> results = sessionize(source, sessionKey, sessionTimeout);
        writeResults(params, results);

        return env;
    }

    protected abstract void writeResults(ParameterTool params, SingleOutputStreamOperator<GroupedMessage> results);
    protected abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params, List<String> sessionKey, Long sessionTimeout);

}