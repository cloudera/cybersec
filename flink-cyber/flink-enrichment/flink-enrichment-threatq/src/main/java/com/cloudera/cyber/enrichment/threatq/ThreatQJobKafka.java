package com.cloudera.cyber.enrichment.threatq;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.Utils;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.serialization.AbstractDeserializationSchema;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.util.Preconditions;

import java.io.ByteArrayInputStream;
import java.util.Properties;

import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_INPUT;
import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_OUTPUT;
import static com.cloudera.cyber.flink.Utils.readKafkaProperties;

public class ThreatQJobKafka extends ThreatQJob {

    private static final String PARAMS_TOPIC_ENRICHMENT_INPUT = "enrichment.topic.input";
    private static final String THREATQ_ENRICHMENT_GROUP_ID = "enrichment-threatq";
    private static final String THREATQ_PARSER_GROUP_ID = "threatq-parser";

    public static void main(String[] args) throws Exception {
        Preconditions.checkArgument(args.length >= 1, "Arguments must consist of a properties files");
        new ThreatQJobKafka().createPipeline(Utils.getParamToolsFromProperties(args)).execute("Enrichments - ThreatQ");
    }

    @Override
    protected void writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<Message> reduction) {
        reduction.addSink(new FlinkUtils<>(Message.class).createKafkaSink(params.getRequired(PARAMS_TOPIC_OUTPUT), THREATQ_ENRICHMENT_GROUP_ID,params))
                .name("Kafka Sink").uid("kafka-sink");
    }

    @Override
    public DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.addSource(
                FlinkUtils.createKafkaSource(params.getRequired(PARAMS_TOPIC_INPUT), params, THREATQ_ENRICHMENT_GROUP_ID)
        ).name("Kafka Source").uid("kafka-source");
    }

    @Override
    protected DataStream<ThreatQEntry> createEnrichmentSource(StreamExecutionEnvironment env, ParameterTool params) {
        String topic = params.getRequired(PARAMS_TOPIC_ENRICHMENT_INPUT);

        Properties kafkaProperties = readKafkaProperties(params, THREATQ_PARSER_GROUP_ID, true);

        FlinkKafkaConsumer<byte[]> source = new FlinkKafkaConsumer<>(topic, new AbstractDeserializationSchema<byte[]>() {
            @Override
            public byte[] deserialize(byte[] bytes) {
                return bytes;
            }
        }, kafkaProperties);
        return env.addSource(source).flatMap(
                (FlatMapFunction<byte[], ThreatQEntry>) (s, collector) ->
                        ThreatQParser.parse(new ByteArrayInputStream(s)).forEach(collector::collect))
                .name("ThreatQ Input").uid("kafka-enrichment-source");
    }
}
