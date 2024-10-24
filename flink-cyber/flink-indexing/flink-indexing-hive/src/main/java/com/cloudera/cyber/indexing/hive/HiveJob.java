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

package com.cloudera.cyber.indexing.hive;

import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.indexing.hive.tableapi.TableApiAbstractJob;
import com.cloudera.cyber.indexing.hive.tableapi.TableApiJobFactory;
import com.cloudera.cyber.scoring.ScoredMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

@Slf4j
public abstract class HiveJob {

    protected StreamExecutionEnvironment createPipeline(ParameterTool params) {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        FlinkUtils.setupEnv(env, params);

        DataStream<ScoredMessage> source = createSource(env, params);
        if (params.get("flink.writer", "").equalsIgnoreCase("TableAPI")) {
            try {
                final String connectorName = params.get("flink.output-connector", "hive");
                final TableApiAbstractJob job =
                      TableApiJobFactory.getJobByConnectorName(connectorName, params, env, source);
                return job.startJob();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        source.addSink(new StreamingHiveSink(params)).name("Hive Streaming Indexer");

        return env;
    }

    protected abstract SingleOutputStreamOperator<ScoredMessage> createSource(StreamExecutionEnvironment env,
                                                                              ParameterTool params);
}
