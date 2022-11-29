package com.cloudera.cyber.enrichment;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.commands.EnrichmentCommandResponse;
import com.cloudera.cyber.enrichemnt.stellar.StellarEnrichmentJob;
import com.cloudera.cyber.enrichment.geocode.IpGeo;
import com.cloudera.cyber.enrichment.hbase.HbaseJob;
import com.cloudera.cyber.enrichment.hbase.HbaseJobRawKafka;
import com.cloudera.cyber.enrichment.lookup.LookupJob;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentConfig;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentKind;
import com.cloudera.cyber.enrichment.rest.RestLookupJob;
import com.cloudera.cyber.enrichment.threatq.ThreatQConfig;
import com.cloudera.cyber.enrichment.threatq.ThreatQEntry;
import com.cloudera.cyber.enrichment.threatq.ThreatQJob;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.scoring.ScoredMessage;
import com.cloudera.cyber.scoring.ScoringJob;
import com.cloudera.cyber.scoring.ScoringRuleCommand;
import com.cloudera.cyber.scoring.ScoringRuleCommandResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static com.cloudera.cyber.enrichment.geocode.IpGeoJob.PARAM_ASN_DATABASE_PATH;
import static com.cloudera.cyber.enrichment.geocode.IpGeoJob.PARAM_ASN_FIELDS;
import static com.cloudera.cyber.enrichment.geocode.IpGeoJob.PARAM_GEO_DATABASE_PATH;
import static com.cloudera.cyber.enrichment.geocode.IpGeoJob.PARAM_GEO_FIELDS;

@Slf4j
public abstract class EnrichmentJob {
    private static final String PARAMS_LOOKUPS_CONFIG_FILE = "lookups.config.file";
    private static final String PARAMS_REST_CONFIG_FILE = "rest.config.file";
    private static final String PARAMS_THREATQ_CONFIG_FILE = "threatq.config.file";

    private static final String PARAMS_ENABLE_GEO = "geo.enabled";
    private static final String PARAMS_ENABLE_ASN = "asn.enabled";
    private static final String PARAMS_ENABLE_HBASE = "hbase.enabled";
    private static final String PARAMS_ENABLE_REST = "rest.enabled";
    private static final String PARAMS_ENABLE_THREATQ = "threatq.enabled";
    private static final String PARAMS_ENABLE_RULES = "rules.enabled";
    private static final String PARAMS_ENABLE_STELLAR = "stellar.enabled";

    protected StreamExecutionEnvironment createPipeline(ParameterTool params) throws IOException {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        FlinkUtils.setupEnv(env, params);

        SingleOutputStreamOperator<Message> messages = createSource(env, params);
        DataStream<EnrichmentCommand> enrichments = createEnrichmentSource(env, params);

        List<EnrichmentConfig> enrichmentConfigs = ConfigUtils.allConfigs(Files.readAllBytes(Paths.get(params.getRequired(PARAMS_LOOKUPS_CONFIG_FILE))));
        SingleOutputStreamOperator<EnrichmentCommand> localEnrichments = enrichments.filter(new FilterEnrichmentType(enrichmentConfigs, EnrichmentKind.LOCAL));
        SingleOutputStreamOperator<EnrichmentCommand> hbaseEnrichments = enrichments.filter(new FilterEnrichmentType(enrichmentConfigs, EnrichmentKind.HBASE));

        SingleOutputStreamOperator<Message> geoEnriched = params.getBoolean(PARAMS_ENABLE_GEO, true) ?
                IpGeo.geo(messages,
                        Arrays.asList(params.getRequired(PARAM_GEO_FIELDS).split(",")),
                        params.getRequired(PARAM_GEO_DATABASE_PATH)) : messages;
        SingleOutputStreamOperator<Message> asnEnriched = params.getBoolean(PARAMS_ENABLE_ASN, true) ?
                IpGeo.asn(geoEnriched,
                        Arrays.asList(params.getRequired(PARAM_ASN_FIELDS).split(",")),
                        params.getRequired(PARAM_ASN_DATABASE_PATH)) : geoEnriched;

        Tuple2<SingleOutputStreamOperator<Message>, DataStream<EnrichmentCommandResponse>> enriched = LookupJob.enrich(localEnrichments, asnEnriched, enrichmentConfigs);

        DataStream<EnrichmentCommandResponse> enrichmentCommandResponses = enriched.f1;

        // write the hbase enrichments to hbase
        if (params.getBoolean(PARAMS_ENABLE_HBASE, true)) {
            DataStream<EnrichmentCommandResponse> hbaseEnrichmentResponses = new HbaseJobRawKafka().writeEnrichments(env, params, hbaseEnrichments);
            if (enrichmentCommandResponses != null) {
                enrichmentCommandResponses = enriched.f1.union(hbaseEnrichmentResponses);
            } else {
                enrichmentCommandResponses = hbaseEnrichmentResponses;
            }
        }

        writeEnrichmentQueryResults(env, params, enrichmentCommandResponses);

        DataStream<Message> hbased = params.getBoolean(PARAMS_ENABLE_HBASE, true) ?
                HbaseJob.enrich(enriched.f0, enrichmentConfigs) : enriched.f0;

        // rest based enrichments
        DataStream<Message> rested = params.getBoolean(PARAMS_ENABLE_REST, true) ?
                RestLookupJob.enrich(hbased, params.getRequired(PARAMS_REST_CONFIG_FILE)) : hbased;

        // Run threatQ integrations
        DataStream<Message> tqed;
        if (params.getBoolean(PARAMS_ENABLE_THREATQ, true)) {
            List<ThreatQConfig> threatQconfigs = ThreatQJob.parseConfigs(Files.readAllBytes(Paths.get(params.getRequired(PARAMS_THREATQ_CONFIG_FILE))));
            log.info("ThreatQ Configs {}", threatQconfigs);
            tqed = ThreatQJob.enrich(rested, threatQconfigs);
            ThreatQJob.ingest(createThreatQSource(env, params), threatQconfigs);
        } else {
            tqed = rested;
        }

        DataStream<Message> stellarStream;
        if (params.getBoolean(PARAMS_ENABLE_STELLAR, true)) {
            String configDir = params.getRequired(StellarEnrichmentJob.PARAMS_CONFIG_DIR);
            String geoDatabasePath = params.getRequired(PARAM_GEO_DATABASE_PATH);
            String asnDatabasePath = params.getRequired(PARAM_ASN_DATABASE_PATH);
            stellarStream = StellarEnrichmentJob.enrich(tqed, StellarEnrichmentJob.loadFiles(configDir), geoDatabasePath, asnDatabasePath);
        } else {
            stellarStream = tqed;
        }

        // disabled by default - NOT IMPLEMENTED
        DataStream<Message> ruled = params.getBoolean(PARAMS_ENABLE_RULES, false) ?
                doRules(stellarStream, params) : stellarStream;

        DataStream<ScoredMessage> scoring = doScoring(ruled, env, params);

        writeResults(env, params, scoring);
        return env;
    }


    /**
     * @param in     Messages incoming for rules processing
     * @param params Global Job Parameters
     * @return incoming stream for now.  Not implemented.
     */
    private DataStream<Message> doRules(DataStream<Message> in, ParameterTool params) {
        return in;
    }

    private DataStream<ScoredMessage> doScoring(DataStream<Message> in, StreamExecutionEnvironment env, ParameterTool params) {
        DataStream<ScoringRuleCommand> rulesSource = createRulesSource(env, params);
        SingleOutputStreamOperator<ScoredMessage> results = ScoringJob.enrich(in, rulesSource);
        writeScoredRuleCommandResult(params, results.getSideOutput(ScoringJob.COMMAND_RESULT_OUTPUT_TAG));
        return results;
    }

    protected abstract SingleOutputStreamOperator<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);

    protected abstract void writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<ScoredMessage> results);

    protected abstract DataStream<EnrichmentCommand> createEnrichmentSource(StreamExecutionEnvironment env, ParameterTool params);

    protected abstract void writeEnrichmentQueryResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<EnrichmentCommandResponse> sideOutput);

    protected abstract DataStream<ThreatQEntry> createThreatQSource(StreamExecutionEnvironment env, ParameterTool params);

    protected MapFunction<Message, Message> getLongTermLookupFunction() {
        return null;
    }

    protected abstract DataStream<ScoringRuleCommand> createRulesSource(StreamExecutionEnvironment env, ParameterTool params);

    protected abstract void writeScoredRuleCommandResult(ParameterTool params, DataStream<ScoringRuleCommandResult> results);

}
