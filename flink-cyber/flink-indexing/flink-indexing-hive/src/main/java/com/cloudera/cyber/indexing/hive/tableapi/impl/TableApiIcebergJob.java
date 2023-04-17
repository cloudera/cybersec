package com.cloudera.cyber.indexing.hive.tableapi.impl;

import com.cloudera.cyber.indexing.TableColumnDto;
import com.cloudera.cyber.indexing.hive.tableapi.TableApiAbstractJob;
import com.cloudera.cyber.scoring.ScoredMessage;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.SqlDialect;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.catalog.hive.HiveCatalog;

import java.io.IOException;
import java.util.List;

public class TableApiIcebergJob extends TableApiAbstractJob {

    private static final String BASE_TABLE_JSON = "base-iceberg-table.json";

    public TableApiIcebergJob(ParameterTool params, StreamExecutionEnvironment env, DataStream<ScoredMessage> source) throws IOException {
        super(params, env, source, "Iceberg", BASE_TABLE_JSON);
    }

    @Override
    protected void setConnectorDialect(StreamTableEnvironment tableEnv) {
        tableEnv.getConfig().setSqlDialect(SqlDialect.HIVE);
    }

    @Override
    protected String buildTableDLL(String tableName, List<TableColumnDto> columnList) {
        return String.join("\n", "CREATE TABLE " + tableName + " ( ",
                getColumnList(columnList),
                ") PARTITIONED BY (",
                " `dt` string, ",
                " `hr` string",
                ") TBLPROPERTIES (",
                "  'connector' = 'iceberg',",
                "  'catalog-database' = 'cyber',",
                "  'catalog-type' = 'hive',",
                "  'catalog-name' = 'iceberg_catalog',",
                "  'engine.hive.enabled' = 'true',",

                "  'partition.time-extractor.timestamp-pattern'='$dt $hr:00:00',",
                "  'sink.partition-commit.trigger'='process-time',",
                "  'sink.partition-commit.delay'='1 h',",
                "  'sink.partition-commit.policy.kind'='metastore,success-file'",
                ")");
    }

    @Override
    protected void registerCatalog(StreamTableEnvironment tableEnv) {
        String name = "iceberg_catalog";
        String defaultDatabase = params.get("hive.dbname", "cyber");
        String hiveConfDir = params.get("hive.confdir", "/etc/hive/conf");

        HiveCatalog hive = new HiveCatalog(name, defaultDatabase, hiveConfDir);
        tableEnv.registerCatalog(name, hive);
        tableEnv.useCatalog(name);
    }

    private static void createAndUseDatabase(StreamTableEnvironment tableEnv, String defaultDatabase) {
        //create database if not exists
        if (!"default".equals(defaultDatabase)) {
            final String createDbSql = "CREATE DATABASE IF NOT EXISTS " + defaultDatabase;
            System.out.println("Creating Flink database for iceberg: " + createDbSql);
            tableEnv.executeSql(createDbSql);
        }
        tableEnv.useDatabase(defaultDatabase);
    }

}
