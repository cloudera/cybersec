package com.cloudera.cyber.enrichemnt.stellar;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Preconditions;

import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_INPUT;
import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_OUTPUT;

public class StellarEnrichmentJobKafka extends StellarEnrichmentJob {


    public static void main(String[] args) throws Exception {
        Preconditions.checkArgument(args.length == 1, "Arguments must consist of a single properties file");
        ParameterTool params = ParameterTool.fromPropertiesFile(args[0]);
        FlinkUtils.executeEnv(new StellarEnrichmentJobKafka().createPipeline(params), "Enrichments - Stellar", params);
    }

    @Override
    protected void writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<Message> reduction) {
        reduction.addSink(new FlinkUtils<>(Message.class).createKafkaSink(params.getRequired(PARAMS_TOPIC_OUTPUT), STELLAR_ENRICHMENT_GROUP_ID, params))
                .name("Kafka Sink").uid("kafka-sink");
    }

    @Override
    public DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.addSource(
                FlinkUtils.createKafkaSource(params.getRequired(PARAMS_TOPIC_INPUT), params, STELLAR_ENRICHMENT_GROUP_ID)
        ).name("Kafka Source").uid("kafka-source");
    }

}
