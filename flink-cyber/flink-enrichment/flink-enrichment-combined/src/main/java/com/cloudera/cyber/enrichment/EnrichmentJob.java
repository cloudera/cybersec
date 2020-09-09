package com.cloudera.cyber.enrichment;

import com.cloudera.cyber.EnrichmentEntry;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.enrichment.lookup.LookupJob;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentConfig;
import com.cloudera.cyber.enrichment.rest.RestEnrichmentConfig;
import com.cloudera.cyber.enrichment.rest.RestLookupJob;
import com.cloudera.cyber.flink.FlinkUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public abstract class EnrichmentJob {
    private static final String PARAMS_LOOKUPS_CONFIG_FILE = "lookups.config.file";
    private static final String PARAMS_REST_CONFIG_FILE = "rest.config.file";

    protected StreamExecutionEnvironment createPipeline(ParameterTool params) throws IOException {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        FlinkUtils.setupEnv(env, params);

        DataStream<Message> messages = createSource(env, params);
        DataStream<EnrichmentEntry> enrichments = createEnrichmentSource(env, params);

        List<RestEnrichmentConfig> restConfig = RestLookupJob.parseConfigs(Files.readAllBytes(Paths.get(params.getRequired(PARAMS_REST_CONFIG_FILE))));
        List<EnrichmentConfig> enrichmentConfigs = ConfigUtils.allConfigs(Files.readAllBytes(Paths.get(params.getRequired(PARAMS_LOOKUPS_CONFIG_FILE))));

        DataStream<Message> enriched = LookupJob.enrich(enrichments, messages, enrichmentConfigs);
        DataStream<Message> rested = RestLookupJob.enrich(enriched, restConfig);
        //DataStream<Message> hbased = HbaseJob.enrich(enriched, env, enrichmentConfigs);

        // TODO - apply the rules based enrichments
        DataStream<Message> ruled = rested;

        writeResults(env, params, ruled);
        return env;
    }
    protected abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);
    protected abstract void writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<Message> results);
    protected abstract DataStream<EnrichmentEntry> createEnrichmentSource(StreamExecutionEnvironment env, ParameterTool params);
}
