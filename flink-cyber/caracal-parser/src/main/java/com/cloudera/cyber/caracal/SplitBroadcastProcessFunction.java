package com.cloudera.cyber.caracal;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.parser.MessageToParse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction;
import org.apache.flink.util.Collector;

import java.io.Serializable;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class SplitBroadcastProcessFunction extends KeyedBroadcastProcessFunction<String, MessageToParse, SplitConfig, Message> implements Serializable {

    @NonNull private final Map<String, SplitConfig> configs;
    private transient Map<String, SplittingFlatMapFunction> splitters = new HashMap<>();
    @NonNull private transient final PrivateKey signKey;

    public SplitBroadcastProcessFunction(Map<String, SplitConfig> configs, PrivateKey signKey) {
        super();
        this.configs = configs;
        this.signKey = signKey;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);

        splitters = configs.entrySet().stream().
                collect(Collectors.toMap(
                        k -> k.getKey(),
                        v -> new SplittingFlatMapFunction(v.getValue(), signKey)
                ));
    }

    @Override
    public void processElement(MessageToParse messageToParse, ReadOnlyContext readOnlyContext, Collector<Message> collector) throws Exception {
        SplittingFlatMapFunction splitter = splitters.get(messageToParse);
        if (splitter == null) {
            throw new RuntimeException(String.format("Splitter not found for topic %s", messageToParse.getTopic()));
        }
        splitter.flatMap(messageToParse, collector);
    }

    @Override
    public void processBroadcastElement(SplitConfig splitConfig, Context context, Collector<Message> collector) throws Exception {
        log.info(String.format("Adding splitter %s on thread %d ", splitConfig, Thread.currentThread().getId()));
        context.getBroadcastState(SplitJob.Descriptors.broadcastState).put(splitConfig.getTopic(), splitConfig);
        splitters.put(splitConfig.getTopic(), new SplittingFlatMapFunction(splitConfig, signKey));
    }
}