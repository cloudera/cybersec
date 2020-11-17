package com.cloudera.cyber.enrichment;

import com.cloudera.cyber.EnrichmentEntry;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.ThreatIntelligence;
import com.cloudera.cyber.enrichment.geocode.IpGeo;
import com.cloudera.cyber.enrichment.hbase.HbaseEnrichmentFunction;
import com.cloudera.cyber.enrichment.hbase.HbaseJobRaw;
import com.cloudera.cyber.enrichment.hbase.HbaseJobRawKafka;
import com.cloudera.cyber.enrichment.lookup.LookupJob;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentConfig;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentKind;
import com.cloudera.cyber.enrichment.rest.RestEnrichmentConfig;
import com.cloudera.cyber.enrichment.rest.RestLookupJob;
import com.cloudera.cyber.enrichment.stix.StixJob;
import com.cloudera.cyber.enrichment.stix.StixResults;
import com.cloudera.cyber.enrichment.stix.parsing.ThreatIntelligenceDetails;
import com.cloudera.cyber.flink.FlinkUtils;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static com.cloudera.cyber.enrichment.geocode.IpGeoJob.*;

public abstract class EnrichmentJob {
    private static final String PARAMS_LOOKUPS_CONFIG_FILE = "lookups.config.file";
    private static final String PARAMS_REST_CONFIG_FILE = "rest.config.file";

    private static final String PARAMS_ENABLE_GEO = "geo.enabled";
    private static final String PARAMS_ENABLE_ASN = "asn.enabled";
    private static final String PARAMS_ENABLE_HBASE = "hbase.enabled";
    private static final String PARAMS_ENABLE_REST = "rest.enabled";
    private static final String PARAMS_ENABLE_STIX = "stix.enabled";
    private static final String PARAMS_ENABLE_RULES = "rules.enabled";

    private DataStream<Message> messages;

    protected StreamExecutionEnvironment createPipeline(ParameterTool params) throws IOException {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        FlinkUtils.setupEnv(env, params);

        DataStream<Message> messages = createSource(env, params);
        DataStream<EnrichmentEntry> enrichments = createEnrichmentSource(env, params);

        List<RestEnrichmentConfig> restConfig = RestLookupJob.parseConfigs(Files.readAllBytes(Paths.get(params.getRequired(PARAMS_REST_CONFIG_FILE))));
        List<EnrichmentConfig> enrichmentConfigs = ConfigUtils.allConfigs(Files.readAllBytes(Paths.get(params.getRequired(PARAMS_LOOKUPS_CONFIG_FILE))));

        DataStream<EnrichmentEntry> localEnrichments = enrichments.filter(new FilterEnrichmentType(enrichmentConfigs, EnrichmentKind.LOCAL));
        DataStream<EnrichmentEntry> hbaseEnrichments = enrichments.filter(new FilterEnrichmentType(enrichmentConfigs, EnrichmentKind.HBASE));

        DataStream<Message> geoEnriched = params.getBoolean(PARAMS_ENABLE_GEO, true) ?
                IpGeo.geo(messages,
                        Arrays.asList(params.getRequired(PARAM_GEO_FIELDS).split(",")),
                        params.getRequired(PARAM_GEO_DATABASE_PATH)) : messages;
        DataStream<Message> asnEnriched = params.getBoolean(PARAMS_ENABLE_ASN, true) ?
                IpGeo.asn(geoEnriched,
                        Arrays.asList(params.getRequired(PARAM_ASN_FIELDS).split(",")),
                        params.getRequired(PARAM_ASN_DATABASE_PATH)) : geoEnriched;

        DataStream<Message> enriched = LookupJob.enrich(localEnrichments, asnEnriched, enrichmentConfigs);

        // write the hbase enrichments to hbase
        if (params.getBoolean(PARAMS_ENABLE_HBASE, true)) {
            new HbaseJobRawKafka().writeEnrichments(env, params, hbaseEnrichments);
        }
        DataStream<Message> hbased = params.getBoolean(PARAMS_ENABLE_HBASE, true) ?
                HbaseJobRaw.enrich(enriched, env, enrichmentConfigs) : enriched;

        // rest based enrichments
        DataStream<Message> rested = params.getBoolean(PARAMS_ENABLE_REST, true) ?
                RestLookupJob.enrich(hbased, restConfig) : hbased;


        // stix process parses incoming stix sources and stores locally, can use long term backup
        // also outputs multiple streams which need sending somewhere
        DataStream<Message> tied = params.getBoolean(PARAMS_ENABLE_STIX, true) ?
                doStix(rested, env, params) : rested;


        // TODO - apply the rules based enrichments
        DataStream<Message> ruled = params.getBoolean(PARAMS_ENABLE_RULES,true) ?
                doRules(tied, params) : tied;

        writeResults(env, params, ruled);
        return env;
    }

    /**
     * @TODO - Add the rules processing engine
     *
     * @param in Messages incoming for rules processing
     * @param params Global Job Parameters
     * @return
     */
    private DataStream<Message> doRules(DataStream<Message> in, ParameterTool params) {
        return in;
    }

    private DataStream<Message> doStix(DataStream<Message> in,  StreamExecutionEnvironment env, ParameterTool params) {
        DataStream<String> stixSource = createStixSource(env, params);
        StixResults stix = StixJob.enrich(in, stixSource, getLongTermLookupFunction(), params);
        writeStixThreats(params, stix.getThreats());
        writeStixDetails(params, stix.getDetails());
        return stix.getResults();
    }

    protected abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);

    protected abstract void writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<Message> results);

    protected abstract DataStream<EnrichmentEntry> createEnrichmentSource(StreamExecutionEnvironment env, ParameterTool params);

    /* STIX related parts */
    protected abstract DataStream<String> createStixSource(StreamExecutionEnvironment env, ParameterTool params);

    protected abstract void writeStixThreats(ParameterTool params, DataStream<ThreatIntelligence> results);

    protected abstract void writeStixDetails(ParameterTool params, DataStream<ThreatIntelligenceDetails> results);

    protected MapFunction<Message, Message> getLongTermLookupFunction() {
        return null;
    }


}
