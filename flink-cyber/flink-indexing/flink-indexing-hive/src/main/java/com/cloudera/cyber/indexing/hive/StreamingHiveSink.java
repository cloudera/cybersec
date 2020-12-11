package com.cloudera.cyber.indexing.hive;

import com.cloudera.cyber.Message;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Counter;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;

@Slf4j
public class StreamingHiveSink extends RichSinkFunction<Message> implements CheckpointedFunction {

    private final ParameterTool params;
    private transient HiveStreamingMessageWriter messageWriter;
    private transient Counter counter;

    public StreamingHiveSink(ParameterTool params) {
        this.params = params;
    }

    public void open(Configuration parameters) throws Exception {
        this.messageWriter =  new HiveStreamingMessageWriter(params);
        messageWriter.connect();
        this.counter = getRuntimeContext().getMetricGroup().counter("messagesToHive");
        super.open(parameters);
    }


    @Override
    public void invoke(Message message, Context context) throws Exception {
        log.debug("Adding message {} to transaction", message);
        counter.inc(messageWriter.addMessageToTransaction(message));
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
