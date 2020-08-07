package com.cloudera.cyber.enrichment.stix;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.ThreatIntelligence;
import com.cloudera.cyber.enrichment.stix.parsing.ThreatIntelligenceDetails;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.Utils;
import org.apache.flink.addons.hbase.HBaseWriteOptions;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;

import java.util.Properties;
import java.util.regex.Pattern;

import static com.cloudera.cyber.flink.FlinkUtils.createKafkaSource;

public class StixJobKafka extends StixJob {
    private static final String GROUP_ID = "cyber-stix";
    private static final String PARAM_INPUT_TOPIC = "stix.input.topic";
    private static final String PARAM_OUTPUT_TOPIC = "stix.output.topic";
    private static final String DEFAULT_INPUT_TOPIC = "stix";
    private static final String PARAM_MARKED_OUTPUT_TOPIC = "output.topic";

    /**
     * This function is used to lookup threat intelligence that is not in the near term cache
     *
     * @return
     */
    @Override
    protected MapFunction<Message, Message> getLongTermLookupFunction() {
        return null;
    }

    @Override
    protected void writeResults(ParameterTool params, DataStream<Message> results) {
        FlinkKafkaProducer<Message> sink = new FlinkUtils<>(Message.class).createKafkaSink(
                params.getRequired(PARAM_MARKED_OUTPUT_TOPIC),
                params);
        results.addSink(sink).name("Kafka Results").uid("kafka.results");
    }

    @Override
    protected void writeStixResults(ParameterTool params, DataStream<ThreatIntelligence> results) {
        FlinkKafkaProducer<ThreatIntelligence> sink = new FlinkUtils<>(ThreatIntelligence.class).createKafkaSink(
                params.getRequired(PARAM_OUTPUT_TOPIC),
                params);
        results.addSink(sink).name("Kafka Stix Results").uid("kafka.results.stix");

        ThreatIntelligenceHBaseSinkFunction hbaseSink = new ThreatIntelligenceHBaseSinkFunction("threatIntelligence");
        hbaseSink.setWriteOptions(HBaseWriteOptions.builder()
                .setBufferFlushIntervalMillis(1000)
                .setBufferFlushMaxRows(1000)
                .setBufferFlushMaxSizeInBytes(1024 * 1024 * 64)
                .build()
        );
        results.addSink(hbaseSink);

        ThreatIndexHbaseSinkFunction indexSink = new ThreatIndexHbaseSinkFunction("threatIndex");
        indexSink.setWriteOptions(HBaseWriteOptions.builder()
                .setBufferFlushIntervalMillis(1000)
                .setBufferFlushMaxRows(1000)
                .setBufferFlushMaxSizeInBytes(1024 * 1024 * 64)
                .build());
        results.addSink(indexSink);
    }

    @Override
    protected void writeDetails(ParameterTool params, DataStream<ThreatIntelligenceDetails> results) {
        // write out to HBase
        ThreatIntelligenceDetailsHBaseSinkFunction hbaseSink = new ThreatIntelligenceDetailsHBaseSinkFunction("threatIntelligence");
        hbaseSink.setWriteOptions(HBaseWriteOptions.builder()
                .setBufferFlushIntervalMillis(1000)
                .setBufferFlushMaxRows(1000)
                .setBufferFlushMaxSizeInBytes(1024 * 1024 * 64)
                .build()
        );
        results.addSink(hbaseSink);
    }

    @Override
    protected DataStream<String> createStixSource(StreamExecutionEnvironment env, ParameterTool params) {
        Properties kafkaProperties = Utils.readKafkaProperties(params, true);
        kafkaProperties.put("group.id", GROUP_ID);

        String topic = params.get(PARAM_INPUT_TOPIC, DEFAULT_INPUT_TOPIC);
        FlinkKafkaConsumer<String> consumer = new FlinkKafkaConsumer<>(topic, new SimpleStringSchema(),  kafkaProperties);

        return env.addSource(consumer).name("Stix Kafka Source").uid("stix-kafka-source");
    }

    @Override
    protected DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        Pattern inputTopic = Pattern.compile(params.getRequired("topic.input"));
        String groupId = "cyber-stix";
        return env.addSource(createKafkaSource(inputTopic,
                params,
                groupId))
                .name("Kafka Source")
                .uid("kafka.input");
    }
}
