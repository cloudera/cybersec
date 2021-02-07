package com.cloudera.cyber.enrichment.load;

import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.flink.FlinkUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;

public abstract class BatchEnrichmentLoader {

    protected StreamExecutionEnvironment runPipeline(ParameterTool params) throws Exception {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        FlinkUtils.setupEnv(env, params);

        DataStream<EnrichmentCommand> enrichmentSource = createEnrichmentSource(env, params);

        writeEnrichments(env, params, enrichmentSource);

        return env;
    }

    protected abstract void writeEnrichments(StreamExecutionEnvironment env, ParameterTool params, DataStream<EnrichmentCommand> enrichmentSource);

   protected abstract DataStream<EnrichmentCommand> createEnrichmentSource(StreamExecutionEnvironment env, ParameterTool params) throws Exception;

}
