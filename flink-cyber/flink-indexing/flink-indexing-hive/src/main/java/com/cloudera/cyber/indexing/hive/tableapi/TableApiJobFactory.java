package com.cloudera.cyber.indexing.hive.tableapi;

import com.cloudera.cyber.indexing.hive.tableapi.impl.TableApiHiveJob;
import com.cloudera.cyber.indexing.hive.tableapi.impl.TableApiKafkaJob;
import com.cloudera.cyber.scoring.ScoredMessage;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.io.IOException;

public class TableApiJobFactory {

    public static TableApiAbstractJob getJobByConnectorName(String typeName, ParameterTool params, StreamExecutionEnvironment env, DataStream<ScoredMessage> source) throws IOException {
        if (typeName == null) {
            throw new RuntimeException("Null job type name provided while the Flink writer is selected as TableAPI");
        }
        switch (typeName.toLowerCase()) {
            case "hive":
                return new TableApiHiveJob(params, env, source);
            case "kafka":
                return new TableApiKafkaJob(params, env, source);
            default:
                throw new RuntimeException(String.format("Unknown job type name [%s] provided while the Flink writer is selected as TableAPI", typeName));
        }
    }

}
