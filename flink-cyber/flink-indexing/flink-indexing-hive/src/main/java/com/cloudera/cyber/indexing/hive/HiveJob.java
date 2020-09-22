package com.cloudera.cyber.indexing.hive;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.java.StreamTableEnvironment;
import org.apache.flink.table.catalog.hive.HiveCatalog;
import org.apache.flink.types.Row;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Slf4j
public abstract class HiveJob {

    private static final String PARAMS_HIVE_URL = "hive.url";
    private static final String DEFAULT_HIVE_URL = "jdbc:hive2://localhost:10000/default";
    private static final String PARAMS_HIVE_TABLE = "hive.table";
    private static final String DEFAULT_HIVE_TABLE = "events";
    private static final String PARAMS_HIVE_CATALOG = "hive.catalog";
    private static final String DEFAULT_HIVE_CATALOG = "default";
    private static final String PARAMS_HIVE_SCHEMA = "hive.schema";
    private static final String DEFAULT_HIVE_SCHEMA = "cyber";

    protected StreamExecutionEnvironment createPipeline(ParameterTool params) {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        FlinkUtils.setupEnv(env, params);

        EnvironmentSettings settings = EnvironmentSettings.newInstance()
                .inStreamingMode()
                .useBlinkPlanner()
                .build();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        String hiveConfDir = "/etc/hive/conf/";

        String catalog = params.get(PARAMS_HIVE_CATALOG, DEFAULT_HIVE_CATALOG);
        String schema = params.get(PARAMS_HIVE_SCHEMA, DEFAULT_HIVE_SCHEMA);
        String table = params.get(PARAMS_HIVE_TABLE, DEFAULT_HIVE_TABLE);

        HiveCatalog hive = new HiveCatalog(catalog, schema, hiveConfDir, "3.1.3");
        tableEnv.registerCatalog("hive", hive);
        tableEnv.useCatalog("hive");

        DataStream<Message> source = createSource(env, params);

        // TODO - apply any filtering and index projection logic here

        // get the schema of the hive table
        // map the input messages to the hive schema
        // write the hive entry

        List<String> fields = getFields(params);

        DataStream<Row> output = source.map(m ->
                Row.of(Stream.concat(
                        Stream.of(m.getId(), m.getTs(), m.getMessage()),
                        fields.stream().map(field -> m.getExtensions().get(field))
                ).toArray())
        );

        List<String> fieldNames = Stream.concat(Arrays.asList("id", "ts", "message").stream(),
                fields.stream()).collect(toList());

        tableEnv.insertInto(table, tableEnv.fromDataStream(output));

        return env;
    }

    protected abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);

    private List<String> getFields(ParameterTool params) {
        String url = params.get(PARAMS_HIVE_URL, DEFAULT_HIVE_URL);
        String catalog = params.get(PARAMS_HIVE_CATALOG, DEFAULT_HIVE_CATALOG);
        String schema = params.get(PARAMS_HIVE_SCHEMA, DEFAULT_HIVE_SCHEMA);
        String table = params.get(PARAMS_HIVE_TABLE, DEFAULT_HIVE_TABLE);
        List<String> fields = new ArrayList<>();
        try (Connection con = DriverManager.getConnection(url)) {
            ResultSet columns = con.getMetaData().getColumns(catalog, schema, table, "");
            while (columns.next()) {
                String name = columns.getString(3);
                Types types = (Types) columns.getObject(4);
                fields.add(name);
            }
            //STEP 6: Clean-up environment
            columns.close();
            log.debug("Hive Metastore at %s, using %s.%s.%s", url, catalog, schema, table);
            log.debug("Hive columns: %s", fields);
        } catch (SQLException e) {
            log.error("Hive metadata fetching error", e);
        }
        return fields;
    }
}
