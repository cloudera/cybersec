package com.cloudera.cyber.indexing.hive.tableapi;

import com.cloudera.cyber.indexing.hive.tableapi.impl.TableApiFilesystemJob;
import com.cloudera.cyber.indexing.hive.tableapi.impl.TableApiHiveJob;
import com.cloudera.cyber.indexing.hive.tableapi.impl.TableApiIcebergJob;
import com.cloudera.cyber.indexing.hive.tableapi.impl.TableApiKafkaJob;
import com.cloudera.cyber.scoring.ScoredMessage;
import java.io.IOException;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class TableApiJobFactory {

    public static final String ICEBERG_INDEX_JOB_CONNECTOR = "iceberg";

    public static TableApiAbstractJob getJobByConnectorName(String typeName, ParameterTool params, StreamExecutionEnvironment env, DataStream<ScoredMessage> source) throws IOException {
        if (typeName == null) {
            throw new RuntimeException("Null job type name provided while the Flink writer is selected as TableAPI");
        }
        return switch (typeName.toLowerCase()) {
            case "hive" -> new TableApiHiveJob(params, env, source);
            case ICEBERG_INDEX_JOB_CONNECTOR -> new TableApiIcebergJob(params, env, source);
            case "kafka" -> new TableApiKafkaJob(params, env, source);
            case "filesystem" -> new TableApiFilesystemJob(params, env, source);
            default ->
                    throw new RuntimeException(String.format("Unknown job type name [%s] provided while the Flink writer is selected as TableAPI", typeName));
        };
    }

}
