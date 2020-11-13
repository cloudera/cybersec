package com.cloudera.cyber.profiler.sql;

import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.profiler.sql.aggregates.Hll;
import com.cloudera.cyber.profiler.sql.catalog.ProfileSourceCatalog;
import org.apache.commons.text.StringSubstitutor;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.environment.ExecutionCheckpointingOptions;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.java.StreamTableEnvironment;
import org.apache.flink.table.expressions.TimeIntervalUnit;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ProfilerJob {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new RuntimeException("Path to the properties file is expected as the only argument.");
        }
        ParameterTool params = ParameterTool.fromPropertiesFile(args[0]);
        StreamExecutionEnvironment env = new ProfilerJob().createPipeline(params);
        FlinkUtils.setupEnv(env, params);
        env.execute("Flink Sessionizer");
    }

    private StreamExecutionEnvironment createPipeline(ParameterTool params) {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        tableEnv.registerFunction("hll", new Hll());

        // register schema registry based catalog with special profiler based restrictions
        // i.e. a custom catalog that only exposed things that can be profiled


        tableEnv.getConfig().getConfiguration().set(
                ExecutionCheckpointingOptions.CHECKPOINTING_MODE, CheckpointingMode.EXACTLY_ONCE);
        tableEnv.getConfig().getConfiguration().set(
                ExecutionCheckpointingOptions.CHECKPOINTING_INTERVAL, Duration.ofSeconds(60));

        ProfileModeSql mode = ProfileModeSql.valueOf("TUMBLING");

        Map<String,String> profileFields = new HashMap<>();
        
        Table results = profile(tableEnv,
                params,
                ProfileModeSql.TUMBLING,
                "name",
                5,
                TimeIntervalUnit.MINUTE,
                "ip_src_addr",
                profileFields,
                "ip_not_local(ip_src_addr)",
                "ip_http");
        return env;
    }


    private Table profile(StreamTableEnvironment tableEnv,
                          ParameterTool params,
                          ProfileModeSql mode,
                          String profileName,
                          Integer intervalValue,
                          TimeIntervalUnit intervalUnit,
                          String profileEntity,
                          Map<String,String> profileFields,
                          String profileFilter,
                          String profileSource) {
        ProfileSourceCatalog profileCatalog = new ProfileSourceCatalog((Map<String, String>) params);
        tableEnv.registerCatalog("profiler", profileCatalog);

        Map<String, String> values = new HashMap<String, String>() {{
            put("profileName", profileName);
            put("intervalUnits", intervalUnit.name());
            put("intervalValue", intervalValue.toString());
            put("profileEntity", profileEntity);
            put("profileFields", profileFields.entrySet().stream()
                    .map(e -> e.getValue() + " as " + e.getKey())
                    .collect(Collectors.joining(",")));
            put("profileSource", profileSource);
            put("profileFilter", profileFilter.isEmpty() ? "" : "WHERE " + profileFilter);
        }};

        Table results = tableEnv.sqlQuery(new StringSubstitutor(values).replace(mode.getSqlText()));
        results.insertInto("profiler.profiles");

        return results;
    }

}
