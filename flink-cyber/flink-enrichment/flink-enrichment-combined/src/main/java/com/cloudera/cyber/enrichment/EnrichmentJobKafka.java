package com.cloudera.cyber.enrichment;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.ThreatIntelligence;
import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.commands.EnrichmentCommandResponse;
import com.cloudera.cyber.enrichment.stix.ThreatIndexHbaseSinkFunction;
import com.cloudera.cyber.enrichment.stix.ThreatIntelligenceDetailsHBaseSinkFunction;
import com.cloudera.cyber.enrichment.stix.ThreatIntelligenceHBaseSinkFunction;
import com.cloudera.cyber.enrichment.stix.parsing.ThreatIntelligenceDetails;
import com.cloudera.cyber.enrichment.threatq.ThreatQEntry;
import com.cloudera.cyber.enrichment.threatq.ThreatQParserFlatMap;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.Utils;
import com.cloudera.cyber.scoring.ScoredMessage;
import com.cloudera.cyber.scoring.ScoringRuleCommand;
import com.cloudera.cyber.scoring.ScoringRuleCommandResult;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.util.Preconditions;

import java.util.Properties;

import static com.cloudera.cyber.enrichment.lookup.LookupJobKafka.PARAMS_QUERY_OUTPUT;
import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_INPUT;
import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_OUTPUT;
import static com.cloudera.cyber.flink.Utils.readKafkaProperties;

public class EnrichmentJobKafka extends EnrichmentJob {

    private static final String PARAMS_TOPIC_ENRICHMENT_INPUT = "enrichment.topic.input";
    private static final String PARAMS_GROUP_ID = "group.id";
    private static final String DEFAULT_GROUP_ID = "enrichment-combined";
    public static final String PARAM_STIX_INPUT_TOPIC = "stix.input.topic";
    public static final String SCORING_RULES_GROUP_ID = "scoring-rules";
    public static final String DEFAULT_STIX_INPUT_TOPIC = "stix";
    public static final String PARAM_STIX_OUTPUT_TOPIC = "stix.output.topic";
    public static final String DEFAULT_STIX_OUTPUT_TOPIC = "stix.output";
    private static final String PARAM_TI_TABLE = "stix.hbase.table";
    private static final String DEFAULT_TI_TABLE = "threatIntelligence";
    private static final String PARAMS_TOPIC_THREATQ_INPUT = "threatq.topic.input";


    public static void main(String[] args) throws Exception {
        Preconditions.checkArgument(args.length == 1, "Arguments must consist of a single properties file");
        ParameterTool params = ParameterTool.fromPropertiesFile(args[0]);
        FlinkUtils.executeEnv(new EnrichmentJobKafka().createPipeline(params),"Triaging Job - default",params);
    }

    @Override
    protected void writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<ScoredMessage> reduction) {
        reduction.addSink(new FlinkUtils<>(ScoredMessage.class).createKafkaSink(params.getRequired(PARAMS_TOPIC_OUTPUT), "enrichments-combined", params))
                .name("Kafka Triaging Scored Sink").uid("kafka-sink");
    }

    @Override
    public SingleOutputStreamOperator<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.addSource(
                FlinkUtils.createKafkaSource(params.getRequired(PARAMS_TOPIC_INPUT), params, params.get(PARAMS_GROUP_ID, DEFAULT_GROUP_ID))
        ).name("Kafka Source").uid("kafka-source");
    }

    @Override
    protected DataStream<EnrichmentCommand> createEnrichmentSource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.addSource(
                new FlinkUtils<>(EnrichmentCommand.class).createKafkaGenericSource(params.getRequired(PARAMS_TOPIC_ENRICHMENT_INPUT), params, params.get(PARAMS_GROUP_ID, DEFAULT_GROUP_ID))
        ).name("Kafka Triaging").uid("kafka-enrichment-source");
    }

    @Override
    protected void writeEnrichmentQueryResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<EnrichmentCommandResponse> sideOutput) {
        sideOutput.addSink(new FlinkUtils<>(EnrichmentCommandResponse.class).createKafkaSink(params.getRequired(PARAMS_QUERY_OUTPUT), "enrichment-combined-command", params))
                .name("Triaging Query Sink").uid("kafka-enrichment-query-sink");
    }

    @Override
    protected DataStream<ThreatQEntry> createThreatQSource(StreamExecutionEnvironment env, ParameterTool params) {
        String topic = params.getRequired(PARAMS_TOPIC_THREATQ_INPUT);
        String groupId = "threatq-parser";

        Properties kafkaProperties = readKafkaProperties(params, groupId, true);

        FlinkKafkaConsumer<String> source = new FlinkKafkaConsumer<>(topic, new SimpleStringSchema(), kafkaProperties);
        return env.addSource(source).name("ThreatQ Source").uid("threatq-kafka")
                .flatMap(new ThreatQParserFlatMap()).name("ThreatQ Parser").uid("threatq-parser");
    }


    @Override
    protected void writeStixThreats(ParameterTool params, DataStream<ThreatIntelligence> results) {
        FlinkKafkaProducer<ThreatIntelligence> sink = new FlinkUtils<>(ThreatIntelligence.class).createKafkaSink(
                params.get(PARAM_STIX_OUTPUT_TOPIC, DEFAULT_STIX_OUTPUT_TOPIC), "stix-threat-intel",
                params);
        results.addSink(sink).name("Kafka Stix Results").uid("kafka.results.stix");

        ThreatIntelligenceHBaseSinkFunction hbaseSink = new ThreatIntelligenceHBaseSinkFunction("threatIntelligence", params);

        results.addSink(hbaseSink);

        ThreatIndexHbaseSinkFunction indexSink = new ThreatIndexHbaseSinkFunction("threatIndex", params);
        results.addSink(indexSink).name("Stix sink").uid("stix-hbase-sink");
    }

    @Override
    protected void writeStixDetails(ParameterTool params, DataStream<ThreatIntelligenceDetails> results) {
        // write out to HBase
        ThreatIntelligenceDetailsHBaseSinkFunction hbaseSink = new ThreatIntelligenceDetailsHBaseSinkFunction(params.get(PARAM_TI_TABLE, DEFAULT_TI_TABLE), params);
        results.addSink(hbaseSink).name("Stix Detail sink").uid("stix-hbase-detail-sink");
    }

    @Override
    protected DataStream<ScoringRuleCommand> createRulesSource(StreamExecutionEnvironment env, ParameterTool params) {
        String topic = params.getRequired("query.input.topic");
        FlinkKafkaConsumer<ScoringRuleCommand> source = new FlinkUtils<>(ScoringRuleCommand.class).createKafkaGenericSource(topic, params, SCORING_RULES_GROUP_ID);
        return env.addSource(source)
                .name("Kafka Score Rule Source")
                .uid("kafka.input.rule.command");
    }

    @Override
    protected void writeScoredRuleCommandResult(ParameterTool params, DataStream<ScoringRuleCommandResult> results) {
        String topic = params.getRequired("query.output.topic");
        FlinkKafkaProducer<ScoringRuleCommandResult> sink = new FlinkUtils<>(ScoringRuleCommandResult.class).createKafkaSink(topic, SCORING_RULES_GROUP_ID, params);
        results.addSink(sink).name("Kafka Score Rule Command Results").uid("kafka.output.rule.command.results");
    }

    @Override
    protected DataStream<String> createStixSource(StreamExecutionEnvironment env, ParameterTool params) {
        Properties kafkaProperties = Utils.readKafkaProperties(params, params.get(PARAMS_GROUP_ID, DEFAULT_GROUP_ID), true);

        String topic = params.get(PARAM_STIX_INPUT_TOPIC, DEFAULT_STIX_INPUT_TOPIC);
        FlinkKafkaConsumer<String> consumer = new FlinkKafkaConsumer<>(topic, new SimpleStringSchema(), kafkaProperties);
        return env.addSource(consumer).name("Stix Kafka Source").uid("stix-kafka-source");
    }


}
