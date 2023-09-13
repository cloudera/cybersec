package com.cloudera.cyber.indexing.hive.tableapi.impl;

import com.cloudera.cyber.indexing.hive.tableapi.TableApiAbstractJob;
import com.cloudera.cyber.scoring.ScoredMessage;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.FormatDescriptor;
import org.apache.flink.table.api.TableDescriptor;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.catalog.hive.HiveCatalog;

import java.io.IOException;


public class TableApiHiveJob extends TableApiAbstractJob {

    private static final String BASE_TABLE_JSON = "base-hive-table.json";

    public TableApiHiveJob(ParameterTool params, StreamExecutionEnvironment env, DataStream<ScoredMessage> source) throws IOException {
        super(params, env, source, "Hive", BASE_TABLE_JSON);
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
        return FormatDescriptor.forFormat("orc").build();
    }

    @Override
    protected void registerCatalog(StreamTableEnvironment tableEnv) {
        String name = "hive";
        String defaultDatabase = params.get("hive.dbname", "cyber");
        String hiveConfDir = params.get("hive.confdir", "/etc/hive/conf");

        HiveCatalog hive = new HiveCatalog(name, defaultDatabase, hiveConfDir);
        tableEnv.registerCatalog(name, hive);
        tableEnv.useCatalog(name);
    }

    @Override
    protected TableDescriptor.Builder fillTableOptions(TableDescriptor.Builder builder) {
        return super.fillTableOptions(builder)
                .option("hive.storage.file-format", "orc");
    }

}
