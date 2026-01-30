package com.cloudera.cyber.indexing.hive.tableapi.impl;

import com.cloudera.cyber.indexing.hive.tableapi.TableApiAbstractJob;
import com.cloudera.cyber.indexing.hive.util.FlinkSchemaUtil;
import com.cloudera.cyber.scoring.ScoredMessage;
import java.io.IOException;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.FormatDescriptor;
import org.apache.flink.table.api.TableDescriptor;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;


public class TableApiFilesystemJob extends TableApiAbstractJob {

    private static final String BASE_TABLE_JSON = "base-hive-table.json";
    private final String format;
    private final String path;

    public TableApiFilesystemJob(ParameterTool params, StreamExecutionEnvironment env, DataStream<ScoredMessage> source)
          throws IOException {
        super(params, env, source, "Filesystem", BASE_TABLE_JSON, getSerializationFormat(params));
        format = getFileFormat(params);
        path = params.getRequired("flink.files.path");
    }

    private static String getFileFormat(ParameterTool params) {
        return params.get("flink.files.format", "json");
    }

    private static FlinkSchemaUtil.SerializationFormat getSerializationFormat(ParameterTool params) {
        String fileFormat = getFileFormat(params);
        FlinkSchemaUtil.SerializationFormat serializationFormat = FlinkSchemaUtil.SerializationFormat.AVRO;
        if (fileFormat.equalsIgnoreCase("orc") || fileFormat.equalsIgnoreCase("parquet")) {
            serializationFormat =  FlinkSchemaUtil.SerializationFormat.HIVE;
        }
        return serializationFormat;
    }

    @Override
    protected StreamExecutionEnvironment jobReturnValue() {
        return null;
    }

    @Override
    protected String getTableConnector() {
        return "filesystem";
    }

    @Override
    protected FormatDescriptor getFormatDescriptor() {
        return FormatDescriptor.forFormat(format).build();
    }

    @Override
    protected void registerCatalog(StreamTableEnvironment tableEnv) {
    }

    @Override
    protected TableDescriptor.Builder fillTableOptions(TableDescriptor.Builder builder) {
        return super.fillTableOptions(builder)
              .option("path", path)
              .option("format", format)
              .option("sink.partition-commit.policy.kind", "success-file");
    }

}
