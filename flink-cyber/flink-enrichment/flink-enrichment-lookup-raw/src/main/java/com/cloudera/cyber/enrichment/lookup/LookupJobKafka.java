package com.cloudera.cyber.enrichment.lookup;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.commands.EnrichmentCommandResponse;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.SourcesWithHeaders;
import com.cloudera.cyber.flink.Utils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Preconditions;

import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_INPUT;
import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_OUTPUT;

public class LookupJobKafka extends LookupJob {

    private static final String PARAMS_TOPIC_ENRICHMENT_INPUT = "enrichment.topic.input";
    public static final String PARAMS_QUERY_OUTPUT = "enrichment.topic.query.output";

    public static void main(String[] args) throws Exception {
        new LookupJobKafka().createPipeline(Utils.getParamToolsFromProperties(args)).execute("Enrichments - Local Lookup");
    }

    @Override
    protected void writeQueryResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<EnrichmentCommandResponse> sideOutput) {
        sideOutput.addSink(new FlinkUtils<>(EnrichmentCommandResponse.class).createKafkaSink(params.getRequired(PARAMS_QUERY_OUTPUT), "enrichments-lookup-command", params))
                .name("Kafka Sink").uid("kafka-sink");
    }

    @Override
    protected void writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<Message> reduction) {
        reduction.addSink(new FlinkUtils<>(Message.class).createKafkaSink(params.getRequired(PARAMS_TOPIC_OUTPUT), "enrichents-lookup", params))
        .name("Kafka Sink").uid("kafka-sink");
    }

    @Override
    public SingleOutputStreamOperator<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.addSource(
                FlinkUtils.createKafkaSource(params.getRequired(PARAMS_TOPIC_INPUT), params, "enrichment-lookups-local")
        ).name("Kafka Source").uid("kafka-source");
    }

    @Override
    protected DataStream<EnrichmentCommand> createEnrichmentSource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.addSource(new SourcesWithHeaders(EnrichmentCommand.class).createSourceWithHeaders(
                params.getRequired(PARAMS_TOPIC_ENRICHMENT_INPUT),
                params, "enrichment-lookups-local"
                ))
                .name("Kafka Enrichments").uid("kafka-enrichment-source");
    }
}
