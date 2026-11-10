package com.cloudera.cyber.ondemand.enrichment;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.enrichment.EnrichmentJobKafka;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.Utils;
import com.cloudera.cyber.indexing.MessageQueryConversionError;
import com.cloudera.cyber.indexing.hive.HiveJob;
import com.cloudera.cyber.indexing.hive.tableapi.TableApiAbstractJob;
import com.cloudera.cyber.indexing.hive.tableapi.TableApiJobFactory;
import com.cloudera.cyber.indexing.hive.tableapi.impl.TableApiIcebergJob;
import com.cloudera.cyber.ondemand.config.OndemandEnrichmentConfiguration;
import com.cloudera.cyber.ondemand.source.decode.MapRowToMessage;
import com.cloudera.cyber.ondemand.source.decode.ReadTableSql;
import com.cloudera.cyber.scoring.ScoredMessage;
import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.types.Row;
import org.apache.flink.util.Preconditions;

import org.apache.flink.table.api.Table;

import java.time.Duration;
import java.util.HashSet;
import java.util.Map;

public class OnDemandPipelineJob extends EnrichmentJobKafka {

    private final OndemandEnrichmentConfiguration configuration;
    private StreamTableEnvironment tableEnv;

    private OnDemandPipelineJob(OndemandEnrichmentConfiguration configuration) {
        this.configuration = configuration;
    }

    public static void main(String[] args) throws Exception {
        Preconditions.checkArgument(args.length >= 1, "Arguments must consist of a properties files");
        ParameterTool params = Utils.getParamToolsFromProperties(args);
        OndemandEnrichmentConfiguration configuration = OndemandEnrichmentConfiguration.readConfiguration(params);
        FlinkUtils.executeEnv(new OnDemandPipelineJob(configuration).createPipeline(configuration.triageParams()),"OnDemandTriage Job - default",params);
    }

    @Override
    public SingleOutputStreamOperator<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {

        // set up the streaming table environment and catalogs
        this.tableEnv = StreamTableEnvironment.create(env);
        String catalogName = TableApiIcebergJob.registerCatalog(tableEnv, configuration.indexParams());
        String databaseName = TableApiIcebergJob.getDatabase(configuration.indexParams());

        // read the unenriched data from the database
        DataStream<Row> resultRowStream = ReadTableSql.generateSelectStatements(configuration, catalogName, databaseName).
                stream().map(sql -> startQuery(tableEnv, sql)).reduce(DataStream::union).
                orElseThrow(() -> new RuntimeException("The configured job does not generate any SQL"));

        // convert the database rows to messages to enrich
        SingleOutputStreamOperator<Message> messageStream = resultRowStream.process(new MapRowToMessage(configuration.indexTableMappingConfiguration().getUnenrichedTopicMapping()));

        // write the error rows out to a file
        DataStream<MessageQueryConversionError> conversionErrors = messageStream.getSideOutput(MapRowToMessage.MESSAGE_QUERY_CONVERSION_ERROR);
        FileSink<MessageQueryConversionError> fileSink = FileSink
                .forRowFormat(new Path(configuration.errorReportPath()), new SimpleStringEncoder<MessageQueryConversionError>("UTF-8"))
                .withRollingPolicy(
                        DefaultRollingPolicy.builder()
                                .withRolloverInterval(Duration.ofMinutes(5))
                                .withInactivityInterval(Duration.ofMinutes(2))
                                .withMaxPartSize(1024 * 1024 * 128) // 128 MB
                                .build()
                )
                .build();
        conversionErrors.sinkTo(fileSink);

        return messageStream;
    }

    @Override
    protected StreamExecutionEnvironment writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<ScoredMessage> results) {
        ParameterTool destinationDatabase = ParameterTool.fromMap(Map.of(TableApiIcebergJob.DATABASE_PARAM_KEY, configuration.destinationDatabase()));
        ParameterTool indexOndemandResultsParams = configuration.indexParams().mergeWith(destinationDatabase);

        try {
            final String connectorName = HiveJob.getIndexOutputConnectorType(indexOndemandResultsParams);
            tableEnv.executeSql("CREATE DATABASE IF NOT EXISTS " + configuration.destinationDatabase());
            final TableApiAbstractJob job = TableApiJobFactory.getJobByConnectorName(connectorName, indexOndemandResultsParams, env, tableEnv, results);
            // Attach the inserts to the pipeline instead of executing them, so that the single
            // env.execute() in main() submits the source read, enrichment and inserts as one job.
            // On-demand enrichment only writes the primary tables for the sources being enriched;
            // the derived unenriched tables are left to the indexing job.
            return job.startJob(false, false, new HashSet<>(configuration.sourcesToEnrich()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

   /* @Override
    protected DataStream<EnrichmentCommand> createEnrichmentSource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.fromData(Collections.emptyList(), TypeInformation.of(EnrichmentCommand.class));
    }

    @Override
    protected void writeEnrichmentQueryResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<EnrichmentCommandResponse> sideOutput) {
        sideOutput.sinkTo(new DiscardingSink<>());
    }

    @Override
    protected DataStream<EnrichmentCommand> createThreatQSource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.fromData(Collections.emptyList(), TypeInformation.of(EnrichmentCommand.class));
    }

    @Override
    protected DataStream<ScoringRuleCommand> createRulesSource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.fromData(Collections.emptyList(), TypeInformation.of(ScoringRuleCommand.class));
    }

    @Override
    protected void writeScoredRuleCommandResult(ParameterTool params, DataStream<ScoringRuleCommandResult> results) {
        results.sinkTo(new DiscardingSink<>());
    }*/

    private DataStream<Row> startQuery(StreamTableEnvironment tableEnv, String sql) {
        System.out.println(sql);
        Table result = tableEnv.sqlQuery(sql);
        return tableEnv.toDataStream(result);
    }
}
