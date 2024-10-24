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

import com.cloudera.cyber.scoring.ScoredMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Counter;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;

@Slf4j
public class StreamingHiveSink extends RichSinkFunction<ScoredMessage> implements CheckpointedFunction {

    private final ParameterTool params;
    private transient HiveStreamingMessageWriter messageWriter;
    private transient Counter counter;

    public StreamingHiveSink(ParameterTool params) {
        this.params = params;
    }

    public void open(Configuration parameters) throws Exception {
        this.messageWriter = new HiveStreamingMessageWriter(params);
        messageWriter.connect();
        this.counter = getRuntimeContext().getMetricGroup().counter("messagesToHive");
        super.open(parameters);
    }


    @Override
    public void invoke(ScoredMessage scoredMessage, Context context) throws Exception {
        log.debug("Adding message {} to transaction", scoredMessage);
        counter.inc(messageWriter.addMessageToTransaction(scoredMessage));
    }

    @Override
    public void close() throws Exception {
        messageWriter.close();
        super.close();
    }

    @Override
    public void snapshotState(FunctionSnapshotContext functionSnapshotContext) throws Exception {
        this.counter.inc(messageWriter.endTransaction());
        messageWriter.commit();
    }

    @Override
    public void initializeState(FunctionInitializationContext functionInitializationContext) {
        // no initialization required
    }
}
