package com.cloudera.cyber.enrichment.stix;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.ThreatIntelligence;
import com.cloudera.cyber.enrichment.stix.parsing.ParsedThreatIntelligence;
import com.cloudera.cyber.enrichment.stix.parsing.Parser;
import com.cloudera.cyber.enrichment.stix.parsing.ThreatIntelligenceDetails;
import com.cloudera.cyber.flink.FlinkUtils;
import com.google.common.collect.Lists;
import org.apache.commons.collections.ListUtils;
import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.StateTtlConfig;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class StixJob {

    private static final String CONFIG_PREFIX = "threatIntelligence";
    private static final int CONFIG_PREFIX_LENGTH = CONFIG_PREFIX.length();


    protected StreamExecutionEnvironment createPipeline(ParameterTool params) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        FlinkUtils.setupEnv(env, params);

        DataStream<String> stixSource = createStixSource(env, params);
        SingleOutputStreamOperator<ParsedThreatIntelligence> stixResults = stixSource.flatMap(new Parser());
        SingleOutputStreamOperator<ThreatIntelligence> threats = stixResults.map(t -> t.getThreatIntelligence());

        MapStateDescriptor<String, List<ThreatIntelligence>> threatIntelligenceState =
                new MapStateDescriptor<String, List<ThreatIntelligence>>("threatIntelligence",
                        Types.STRING, Types.LIST(TypeInformation.of(ThreatIntelligence.class)));
        threatIntelligenceState.enableTimeToLive(StateTtlConfig.newBuilder(Time.days(30)).build());

        // Broadcast version is for short term and includes most recent TI
        BroadcastStream<ThreatIntelligence> broadcast = threats.broadcast(threatIntelligenceState);
        DataStream<Message> source = createSource(env, params);

        Map<String, List<String>> fieldsToType = getFieldsMappings(params);

        SingleOutputStreamOperator<Message> results = source.connect(broadcast)
                .process(new ThreatIntelligenceBroadcastProcessFunction(threatIntelligenceState, fieldsToType))
                .map(getLongTermLookupFunction());

        writeResults(params, results);

        writeStixResults(params, threats);
        writeDetails(params, stixResults.map(t -> ThreatIntelligenceDetails.newBuilder()
                .setId(t.getThreatIntelligence().getId())
                .setStixSource(t.getSource())
                .build()));

        return env;
    }

    /**
     * Config field mappings as prefix.field.some_index_which_is_thrown_away=cybox_object_type
     *
     * @param params
     * @return
     */
    @VisibleForTesting
    protected static Map<String, List<String>> getFieldsMappings(ParameterTool params) {
        return params.getProperties().stringPropertyNames().stream()
                .filter(p -> p.startsWith(CONFIG_PREFIX))
                .map(p -> p.substring(CONFIG_PREFIX_LENGTH + 1).split("\\."))
                .map(p -> Arrays.asList(p))
                .collect(Collectors.toMap(
                        p -> p.size() == 1 ? p.get(0) : String.join(".", p.subList(0, p.size() - 1)),
                        v -> Lists.newArrayList(params.get(CONFIG_PREFIX + "." + String.join(".", v))),
                        (l1, l2) -> ListUtils.union(l1,l2)
                ));
    }

    protected abstract MapFunction<Message, Message> getLongTermLookupFunction();

    protected abstract void writeResults(ParameterTool params, DataStream<Message> results);

    protected abstract void writeStixResults(ParameterTool params, DataStream<ThreatIntelligence> results);

    protected abstract void writeDetails(ParameterTool params, DataStream<ThreatIntelligenceDetails> results);

    protected abstract DataStream<String> createStixSource(StreamExecutionEnvironment env, ParameterTool params);

    protected abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);
}
