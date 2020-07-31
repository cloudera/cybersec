package com.cloudera.cyber.caracal;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.parser.MessageToParse;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction;
import org.apache.flink.util.Collector;

import java.util.List;
import java.util.Map;

public abstract class SplitJob {

    protected StreamExecutionEnvironment createPipeline(ParameterTool params) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        DataStream<MessageToParse> source = createSource(env, params);

        List<SplitConfig> configs = parseConfig("");

        BroadcastStream<SplitConfig> configStream =
                env.fromCollection(configs)
                        .keyBy(k -> k.getTopic())
                        .broadcast(Descriptors.broadcastState);

        results = source
                .keyBy(k -> k.getTopic())
                .connect(configStream)
                .process(new KeyedBroadcastProcessFunction<String, MessageToParse, SplitConfig, Message>() {

                    @Override
                    public void processElement(MessageToParse messageToParse, ReadOnlyContext readOnlyContext, Collector<Message> collector) throws Exception {
                        messageToParse.getOriginalSource();
                    }

                    @Override
                    public void processBroadcastElement(SplitConfig splitConfig, Context context, Collector<Message> collector) throws Exception {
                        // add the config
                    }
                })

/*                .flatMap(new SplittingFlatMapFunction(config.getSplitPath(), config.getHeaderPath(), config.getTimestampField(), SplittingFlatMapFunction.TimestampSource.HEADER));
        writeResults(params, results);
*/


        //printResults(results);

        return env;
    }

    protected static class Descriptors {
        public static MapStateDescriptor<?, ?> broadcastState = new MapStateDescriptor<String, SplitConfig>("configs", String.class, SplitConfig.class);
    }

    protected abstract List<SplitConfig> parseConfig(String s);

    private void printResults(SingleOutputStreamOperator<Message> results) {
        results.print();
    }

    protected abstract void writeResults(ParameterTool params, DataStream<Message> results);

    protected abstract DataStream<MessageToParse> createSource(StreamExecutionEnvironment env, ParameterTool params);
}
