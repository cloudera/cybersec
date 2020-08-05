package com.cloudera.cyber.profiler;
import com.cloudera.cyber.Message;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;

public class ProfilerSource extends RichSourceFunction<Message> {
    public ProfilerSource(String source) {
    }

    @Override
    public void run(SourceContext<Message> sourceContext) throws Exception {

    }

    @Override
    public void cancel() {

    }
}
