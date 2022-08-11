package com.cloudera.cyber.enrichment.rest;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.Utils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Preconditions;

import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_INPUT;
import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_OUTPUT;

public class RestLookupJobKafka extends RestLookupJob {

    private static final String DEFAULT_GROUP_ID = "enrichment-rest";
    public static void main(String[] args) throws Exception {
        new RestLookupJobKafka().createPipeline(Utils.getParamToolsFromProperties(args)).execute("Enrichment - REST");
    }

    @Override
    protected DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.addSource(FlinkUtils.createKafkaSource(params.getRequired(PARAMS_TOPIC_INPUT), params, DEFAULT_GROUP_ID))
                .name("Kafka Source").uid("kafka-source");
    }

    @Override
    protected void writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<Message> results) {
        results.addSink(new FlinkUtils<>(Message.class).createKafkaSink(params.getRequired(PARAMS_TOPIC_OUTPUT), DEFAULT_GROUP_ID, params))
                .name("Kafka Sink").uid("kafka-sink");
    }
}
