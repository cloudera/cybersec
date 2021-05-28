package com.cloudera.cyber.enrichment.hbase;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageTypeFactory;
import com.cloudera.cyber.commands.CommandType;
import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.flink.FlinkUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.hbase.sink.HBaseSinkFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Preconditions;

import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_INPUT;
import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_OUTPUT;

public class HbaseJobRawKafka extends HbaseJob {
    private static final String PARAMS_ENRICHMENT_TABLE = "enrichments.table";
    private static final String PARAMS_TOPIC_ENRICHMENT_INPUT = "enrichment.topic.input";
    private static final String DEFAULT_GROUP_ID = "enrichment-lookups-hbase";

    public static void main(String[] args) throws Exception {
        Preconditions.checkArgument(args.length == 1, "Arguments must consist of a single properties file");
        new HbaseJobRawKafka().createPipeline(ParameterTool.fromArgs(args)).execute("Enrichments - HBase Lookup");
    }

    @Override
    public void writeEnrichments(StreamExecutionEnvironment env, ParameterTool params, DataStream<EnrichmentCommand> enrichmentSource) {
        // filter out commands that don't require changes to Hbase
        DataStream<EnrichmentCommand> hbaseMods = enrichmentSource.filter(c -> (c.getType().equals(CommandType.ADD) || c.getType().equals(CommandType.DELETE)));
        HBaseSinkFunction<EnrichmentCommand> hbaseSink = new HBaseEnrichmentCommandSink(params.getRequired(PARAMS_ENRICHMENT_TABLE), params);
        hbaseMods.addSink(hbaseSink);
    }

    @Override
    protected void writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<Message> reduction) {
        reduction.addSink(new FlinkUtils<>(Message.class).createKafkaSink(params.getRequired(PARAMS_TOPIC_OUTPUT), DEFAULT_GROUP_ID,params));
    }

    @Override
    public DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.addSource(
                FlinkUtils.createKafkaSource(params.getRequired(PARAMS_TOPIC_INPUT), params, DEFAULT_GROUP_ID)
        ).returns(new MessageTypeFactory().createTypeInfo(null, null));
    }

    @Override
    protected DataStream<EnrichmentCommand> createEnrichmentSource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.addSource(
                new FlinkUtils<>(EnrichmentCommand.class).createKafkaGenericSource(params.getRequired(PARAMS_TOPIC_ENRICHMENT_INPUT), params, DEFAULT_GROUP_ID)
        );
    }
}
