package com.cloudera.cyber.indexing.hive.tableapi.impl;

import com.cloudera.cyber.indexing.hive.tableapi.TableApiAbstractJob;
import com.cloudera.cyber.indexing.hive.util.FlinkSchemaUtil;
import com.cloudera.cyber.scoring.ScoredMessage;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.FormatDescriptor;
import org.apache.flink.table.api.TableDescriptor;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.catalog.CatalogDescriptor;
import org.apache.flink.table.catalog.Column;
import org.apache.flink.table.catalog.ResolvedSchema;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class TableApiIcebergJob extends TableApiAbstractJob {

    private static final String BASE_TABLE_JSON = "base-hive-table.json";

    public TableApiIcebergJob(ParameterTool params, StreamExecutionEnvironment env, DataStream<ScoredMessage> source) throws IOException {
        super(params, env, source, "Iceberg", BASE_TABLE_JSON, FlinkSchemaUtil.SerializationFormat.ICEBERG );
    }

    @Override
    protected void registerCatalog(StreamTableEnvironment tableEnv) {
        String catalogName = "iceberg";
        String defaultDatabase = params.get("hive.dbname", "cyber");
        String hiveConfDir = params.get("hive.confdir", "/etc/hive/conf");

        Configuration catalogConfig = new Configuration();
        catalogConfig.setString("type", catalogName);
        catalogConfig.setString("catalog-type", "hive");
        catalogConfig.setString("engine.hive.enabled", "true");
        catalogConfig.setString("default-database", defaultDatabase);
        catalogConfig.setString("hive-conf-dir", hiveConfDir);

        tableEnv.createCatalog(catalogName, CatalogDescriptor.of(catalogName, catalogConfig));
        tableEnv.useCatalog(catalogName);
    }

    @Override
    protected StreamExecutionEnvironment jobReturnValue() {
        return null;
    }

    @Override
    protected String getTableConnector() {
        return "hive";
    }

    @Override
    protected FormatDescriptor getFormatDescriptor() {
        return null;
    }

    @Override
    protected TableDescriptor.Builder fillTableOptions(TableDescriptor.Builder builder) {
        return super.fillTableOptions(builder)
                .option("format-version", "2").option("write.upsert.enabled", "false");
    }

    @Override
    protected ResolvedSchema handleMissingColumns(StreamTableEnvironment tableEnv, String connectorName, String tableName, List<Column> missingColumns) {
        String newColumns = missingColumns.stream().map(col -> String.join(" ", col.getName(), col.getDataType().toString())).collect(Collectors.joining(", "));
        String addColumnsTableAlter = String.format("ALTER TABLE %s ADD (%s)", tableName, newColumns);
        System.out.printf("Altering table to match data produced: %s\n", addColumnsTableAlter);
        TableResult alterColumnsResult = tableEnv.executeSql(addColumnsTableAlter);
        ResolvedSchema alteredSchema = tableEnv.from(tableName).getResolvedSchema();
        System.out.printf("Schema after alter table: %s\n", alteredSchema.toString());

        return alteredSchema;
    }
}
