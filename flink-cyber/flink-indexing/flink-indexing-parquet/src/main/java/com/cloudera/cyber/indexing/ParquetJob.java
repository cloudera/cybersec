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
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public abstract class ParquetJob {

    protected StreamExecutionEnvironment createPipeline(ParameterTool params) {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        FlinkUtils.setupEnv(env, params);

        DataStream<Message> source = createSource(env, params);

        // TODO - apply any filtering and index projection logic here

        // now add the correctly formed version for tables

        // this will read an expected schema and map the incoming messages by type to the expected output fields
        // unknown fields will either be dumped in a 'custom' map field or dropped based on config


        writeResults(source, params);

        return env;
    }
    protected abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);
    protected abstract void writeResults(DataStream<Message> results, ParameterTool params);
    protected abstract void writeTabularResults(DataStream<Message> results, String path, ParameterTool params);
}
