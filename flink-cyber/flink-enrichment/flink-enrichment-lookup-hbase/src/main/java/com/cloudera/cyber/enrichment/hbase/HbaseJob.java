package com.cloudera.cyber.enrichment.hbase;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentConfig;
import com.cloudera.cyber.flink.FlinkUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static com.cloudera.cyber.enrichment.ConfigUtils.PARAMS_CONFIG_FILE;
import static com.cloudera.cyber.enrichment.ConfigUtils.allConfigs;

public abstract class HbaseJob {

    public static DataStream<Message> enrich(DataStream<Message> source, List<EnrichmentConfig> configs) {
        return source.map(new HbaseEnrichmentMapFunction(configs, "enrichments"))
                .name("HBase Enrichment Mapper").uid("hbase-map");
    }

    protected StreamExecutionEnvironment createPipeline(ParameterTool params) throws IOException {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        FlinkUtils.setupEnv(env, params);

        DataStream<Message> source = createSource(env, params);
        DataStream<EnrichmentCommand> enrichmentSource = createEnrichmentSource(env, params);

        byte[] configJson = Files.readAllBytes(Paths.get(params.getRequired(PARAMS_CONFIG_FILE)));

        DataStream<Message> result = enrich(source, allConfigs(configJson));
        writeResults(env, params, result);
        writeEnrichments(env, params, enrichmentSource);

        return env;
    }

    public abstract void writeEnrichments(StreamExecutionEnvironment env, ParameterTool params, DataStream<EnrichmentCommand> enrichmentSource);

    protected abstract void writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<Message> result);

    protected abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);

    protected abstract DataStream<EnrichmentCommand> createEnrichmentSource(StreamExecutionEnvironment env, ParameterTool params);
}

