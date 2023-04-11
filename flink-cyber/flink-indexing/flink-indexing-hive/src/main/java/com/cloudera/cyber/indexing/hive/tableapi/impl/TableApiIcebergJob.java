package com.cloudera.cyber.indexing.hive.tableapi.impl;

import com.cloudera.cyber.indexing.TableColumnDto;
import com.cloudera.cyber.indexing.hive.tableapi.TableApiAbstractJob;
import com.cloudera.cyber.scoring.ScoredMessage;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.SqlDialect;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

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
                ", `dt` string, ",
                " `hr` string",
                ") PARTITIONED BY (",
                " `dt`, ",
                " `hr`",
                ")");
    }

    @Override
    protected void registerCatalog(StreamTableEnvironment tableEnv) {
        String defaultDatabase = params.get("hive.dbname", "cyber");
        String hiveConfDir = params.get("hive.confdir", "/etc/hive/conf");
        String hiveMetastoreUri = params.get("hive.metastore.uri", "thrift://localhost:9083");

        final String catalogSql = String.join("\n", "CREATE CATALOG iceberg_catalog WITH (",
                "  'type'='iceberg',",
                "  'catalog-type'='hive',",
                "  'uri'='" + hiveMetastoreUri + "',",
                "  'hive-conf-dir'='" + hiveConfDir + "',",
                "  'clients'='5',",
                "  'property-version'='1'",
                ")");
        System.out.println("Creating Flink catalog for iceberg: " + catalogSql);
        tableEnv.executeSql(catalogSql);

        createAndUseDatabase(tableEnv, defaultDatabase);

        tableEnv.useCatalog("iceberg_catalog");
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
