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
import com.cloudera.cyber.flink.Utils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.core.fs.Path;
import org.apache.flink.formats.parquet.avro.ParquetAvroWriters;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink;
import org.apache.flink.util.Preconditions;

import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_INPUT;

public class ParquetJobKafka extends ParquetJob {

    private static final String PARAMS_OUTPUT_BASEPATH = "index.basepath";

    public static void main(String[] args) throws Exception {
        Preconditions.checkArgument(args.length >= 1, "Arguments must consist of a properties files");
        new ParquetJobKafka().createPipeline(Utils.getParamToolsFromProperties(args)).execute("Indexing - Parquet");
    }

    @Override
    public DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.addSource(
                new FlinkUtils(Message.class).createKafkaSource(params.getRequired(PARAMS_TOPIC_INPUT), params, "indexer-parquet")
        ).name("Kafka Source").uid("kafka-source");
    }

    @Override
    protected void writeResults(DataStream<Message> results, ParameterTool params) {
        final StreamingFileSink<Message> sink = StreamingFileSink
                .forBulkFormat(new Path(params.getRequired(PARAMS_OUTPUT_BASEPATH)),
                        ParquetAvroWriters.forSpecificRecord(Message.class))
                .build();
        results.addSink(sink).name("Parquet Sink").uid("parquet-sink");
    }

    @Override
    protected void writeTabularResults(DataStream<Message> results, String path, ParameterTool params) {

    }
}
