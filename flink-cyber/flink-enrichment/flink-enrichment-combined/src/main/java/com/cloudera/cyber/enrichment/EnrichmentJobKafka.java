package com.cloudera.cyber.enrichment;

import com.cloudera.cyber.EnrichmentEntry;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.ThreatIntelligence;
import com.cloudera.cyber.enrichment.stix.ThreatIndexHbaseSinkFunction;
import com.cloudera.cyber.enrichment.stix.ThreatIntelligenceDetailsHBaseSinkFunction;
import com.cloudera.cyber.enrichment.stix.ThreatIntelligenceHBaseSinkFunction;
import com.cloudera.cyber.enrichment.stix.parsing.ThreatIntelligenceDetails;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.Utils;
import org.apache.flink.addons.hbase.HBaseWriteOptions;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.util.Preconditions;

import java.util.Properties;

import static com.cloudera.cyber.enrichment.stix.StixJobKafka.PARAM_STIX_OUTPUT_TOPIC;
import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_INPUT;
import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_OUTPUT;

public class EnrichmentJobKafka extends EnrichmentJob {

    private static final String PARAMS_TOPIC_ENRICHMENT_INPUT = "enrichment.topic.input";
    private static final String PARAMS_GROUP_ID = "group.id";
    private static final String DEFAULT_GROUP_ID = "enrichment-combined";
    public static final String PARAM_STIX_INPUT_TOPIC = "stix.input.topic";
    public static final String DEFAULT_STIX_INPUT_TOPIC = "stix";
    public static final String PARAM_STIX_OUTPUT_TOPIC = "stix.output.topic";
    public static final String DEFAULT_STIX_OUTPUT_TOPIC = "stix.output";
    private static final String PARAM_TI_TABLE = "stix.hbase.table";
    private static final String DEFAULT_TI_TABLE = "threatIntelligence";


    public static void main(String[] args) throws Exception {
        Preconditions.checkArgument(args.length == 1, "Arguments must consist of a single properties file");
        new EnrichmentJobKafka().createPipeline(ParameterTool.fromPropertiesFile(args[0])).execute("Enrichments - Combined");
    }
    @Override
    protected void writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<Message> reduction) {
        reduction.addSink(new FlinkUtils<>(Message.class).createKafkaSink(params.getRequired(PARAMS_TOPIC_OUTPUT), params))
                .name("Kafka Sink").uid("kafka-sink");
    }

    @Override
    public DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.addSource(
                new FlinkUtils(Message.class).createKafkaSource(params.getRequired(PARAMS_TOPIC_INPUT), params, params.get(PARAMS_GROUP_ID, DEFAULT_GROUP_ID))
        ).name("Kafka Source").uid("kafka-source");
    }

    @Override
    protected DataStream<EnrichmentEntry> createEnrichmentSource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.addSource(
                new FlinkUtils(EnrichmentEntry.class).createKafkaGenericSource(params.getRequired(PARAMS_TOPIC_ENRICHMENT_INPUT), params, params.get(PARAMS_GROUP_ID, DEFAULT_GROUP_ID))
        ).name("Kafka Enrichments").uid("kafka-enrichment-source");
    }



    @Override
    protected void writeStixThreats(ParameterTool params, DataStream<ThreatIntelligence> results) {
        FlinkKafkaProducer<ThreatIntelligence> sink = new FlinkUtils<>(ThreatIntelligence.class).createKafkaSink(
                params.get(PARAM_STIX_OUTPUT_TOPIC, DEFAULT_STIX_OUTPUT_TOPIC),
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
    protected void writeStixDetails(ParameterTool params, DataStream<ThreatIntelligenceDetails> results) {
        // write out to HBase
        ThreatIntelligenceDetailsHBaseSinkFunction hbaseSink = new ThreatIntelligenceDetailsHBaseSinkFunction(params.get(PARAM_TI_TABLE, DEFAULT_TI_TABLE));
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
        kafkaProperties.put("group.id", params.get(PARAMS_GROUP_ID, DEFAULT_GROUP_ID));

        String topic = params.get(PARAM_STIX_INPUT_TOPIC, DEFAULT_STIX_INPUT_TOPIC);
        FlinkKafkaConsumer<String> consumer = new FlinkKafkaConsumer<>(topic, new SimpleStringSchema(),  kafkaProperties);

        return env.addSource(consumer).name("Stix Kafka Source").uid("stix-kafka-source");
    }


}
