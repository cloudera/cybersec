package com.cloudera.cyber.enrichment.load;

import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentsConfig;
import com.cloudera.cyber.flink.FlinkUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public abstract class BatchEnrichmentLoader {

    protected StreamExecutionEnvironment runPipeline(ParameterTool params) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        FlinkUtils.setupEnv(env, params);
        load(env, params);
        return env;
    }

    protected abstract void writeResults(ParameterTool params, EnrichmentsConfig enrichmentsConfig,String enrichmentType, DataStream<EnrichmentCommand> enrichmentSource, StreamExecutionEnvironment env);

    protected abstract void load(StreamExecutionEnvironment env, ParameterTool params) throws Exception;

}
