/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

package com.cloudera.cyber.enrichment;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.commands.EnrichmentCommandResponse;
import com.cloudera.cyber.enrichemnt.stellar.StellarEnrichmentJob;
import com.cloudera.cyber.enrichment.cidr.IpRegionCidr;
import com.cloudera.cyber.enrichment.geocode.IpGeo;
import com.cloudera.cyber.enrichment.hbase.HbaseJob;
import com.cloudera.cyber.enrichment.hbase.HbaseJobRawKafka;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentsConfig;
import com.cloudera.cyber.enrichment.lookup.LookupJob;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentConfig;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentKind;
import com.cloudera.cyber.enrichment.rest.RestLookupJob;
import com.cloudera.cyber.enrichment.threatq.ThreatQConfig;
import com.cloudera.cyber.enrichment.threatq.ThreatQJob;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.scoring.ScoredMessage;
import com.cloudera.cyber.scoring.ScoringJob;
import com.cloudera.cyber.scoring.ScoringRuleCommand;
import com.cloudera.cyber.scoring.ScoringRuleCommandResult;
import lombok.extern.slf4j.Slf4j;
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

import static com.cloudera.cyber.enrichment.cidr.IpRegionCidrJob.PARAM_CIDR_CONFIG_PATH;
import static com.cloudera.cyber.enrichment.cidr.IpRegionCidrJob.PARAM_CIDR_IP_FIELDS;
import static com.cloudera.cyber.enrichment.geocode.IpGeoJob.*;
import static com.cloudera.cyber.enrichment.hbase.HbaseJob.PARAMS_ENRICHMENT_CONFIG;

@Slf4j
public abstract class EnrichmentJob {
    private static final String PARAMS_LOOKUPS_CONFIG_FILE = "lookups.config.file";
    private static final String PARAMS_REST_CONFIG_FILE = "rest.config.file";
    private static final String PARAMS_THREATQ_CONFIG_FILE = "threatq.config.file";

    private static final String PARAMS_ENABLE_GEO = "geo.enabled";
    private static final String PARAMS_ENABLE_ASN = "asn.enabled";
    private static final String PARAMS_ENABLE_CIDR = "cidr.enabled";
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

        SingleOutputStreamOperator<Message> cidrEnriched = params.getBoolean(PARAMS_ENABLE_CIDR, false) ?
            IpRegionCidr.cidr(asnEnriched,
                Arrays.asList(params.getRequired(PARAM_CIDR_IP_FIELDS).split(",")),
                params.getRequired(PARAM_CIDR_CONFIG_PATH)) : asnEnriched;

        Tuple2<DataStream<Message>, DataStream<EnrichmentCommandResponse>> enriched = LookupJob.enrich(localEnrichments, cidrEnriched, enrichmentConfigs);

        DataStream<EnrichmentCommandResponse> enrichmentCommandResponses = enriched.f1;

        boolean hbaseEnabled = params.getBoolean(PARAMS_ENABLE_HBASE, true);
        boolean threatqEnabled = params.getBoolean(PARAMS_ENABLE_THREATQ, true);

        EnrichmentsConfig enrichmentsStorageConfig = null;
        if (hbaseEnabled || threatqEnabled) {
            enrichmentsStorageConfig = EnrichmentsConfig.load(params.getRequired(PARAMS_ENRICHMENT_CONFIG));
        }


        DataStream<Message> hbased = hbaseEnabled ?
                HbaseJob.enrich(enriched.f0, enrichmentConfigs, enrichmentsStorageConfig) : enriched.f0;

        // rest based enrichments
        DataStream<Message> rested = params.getBoolean(PARAMS_ENABLE_REST, true) ?
                RestLookupJob.enrich(hbased, params.getRequired(PARAMS_REST_CONFIG_FILE)) : hbased;

        // Run threatQ integrations
        DataStream<Message> tqed;
        DataStream<EnrichmentCommand> threatqEnrichments = null;
        if (threatqEnabled) {
            List<ThreatQConfig> threatQconfigs = ThreatQJob.parseConfigs(Files.readAllBytes(Paths.get(params.getRequired(PARAMS_THREATQ_CONFIG_FILE))));
            log.info("ThreatQ Configs {}", threatQconfigs);
            tqed = ThreatQJob.enrich(rested, threatQconfigs, enrichmentsStorageConfig);
            threatqEnrichments = createThreatQSource(env, params);
        } else {
            tqed = rested;
        }

        DataStream<EnrichmentCommandResponse> hbaseTqEnrichResults = null;
        if (hbaseEnabled || threatqEnabled) {
            hbaseTqEnrichResults = writeHbaseThreatQEnrichmentsToHbaseAndRespond(params, env, hbaseEnrichments, threatqEnrichments, enrichmentsStorageConfig);
        }


        // write the local, hbase, and threatq responses to the output topic
        writeEnrichmentQueryResults(env, params, unionStreams(enrichmentCommandResponses, hbaseTqEnrichResults));

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
                doRules(stellarStream) : stellarStream;

        DataStream<ScoredMessage> scoring = doScoring(ruled, env, params);

        writeResults(env, params, scoring);
        return env;
    }

    private DataStream<EnrichmentCommandResponse> writeHbaseThreatQEnrichmentsToHbaseAndRespond(ParameterTool params, StreamExecutionEnvironment env,
                                                                                                DataStream<EnrichmentCommand> hbaseEnrichments, DataStream<EnrichmentCommand> threatqEnrichments,
                                                                                                EnrichmentsConfig enrichmentsStorageConfig) {

        // write the threatq and hbase enrichments to hbase
        return new HbaseJobRawKafka().writeEnrichments(env, params, unionStreams(hbaseEnrichments, threatqEnrichments), enrichmentsStorageConfig);
    }

    private static<T> DataStream<T> unionStreams(DataStream<T> stream1, DataStream<T> stream2) {
        if (stream1 != null && stream2 != null) {
            return stream1.union(stream2);
        } else if (stream1 != null) {
            return stream1;
        } else {
            return stream2;
        }
    }

    /**
     * @param in     Messages incoming for rules processing
     * @return incoming stream for now.  Not implemented.
     */
    private DataStream<Message> doRules(DataStream<Message> in) {
        return in;
    }

    private DataStream<ScoredMessage> doScoring(DataStream<Message> in, StreamExecutionEnvironment env, ParameterTool params) {
        DataStream<ScoringRuleCommand> rulesSource = createRulesSource(env, params);
        SingleOutputStreamOperator<ScoredMessage> results = ScoringJob.enrich(in, rulesSource, params);
        writeScoredRuleCommandResult(params, results.getSideOutput(ScoringJob.COMMAND_RESULT_OUTPUT_TAG));
        return results;
    }

    protected abstract SingleOutputStreamOperator<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);

    protected abstract void writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<ScoredMessage> results);

    protected abstract DataStream<EnrichmentCommand> createEnrichmentSource(StreamExecutionEnvironment env, ParameterTool params);

    protected abstract void writeEnrichmentQueryResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<EnrichmentCommandResponse> sideOutput);

    protected abstract DataStream<EnrichmentCommand> createThreatQSource(StreamExecutionEnvironment env, ParameterTool params);

    protected abstract DataStream<ScoringRuleCommand> createRulesSource(StreamExecutionEnvironment env, ParameterTool params);

    protected abstract void writeScoredRuleCommandResult(ParameterTool params, DataStream<ScoringRuleCommandResult> results);

}
