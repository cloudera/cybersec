package com.cloudera.cyber.enrichment.hbase;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.TestUtils;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.test.util.JobTester;
import org.apache.flink.test.util.ManualSource;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class QuickTest {

    @Test
    public void testing() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        ManualSource<Message> ms = JobTester.createManualSource(env, TypeInformation.of(Message.class));

        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build());

        //.map(m->m).returns(TypeInformation.of(Message.class));

        DataStream<Message> source = ms.getDataStream();
        tableEnv.createTemporaryView("messages", source);
        Table results = tableEnv.sqlQuery("select * from messages");
        DataStream<Message> outStream = tableEnv.toAppendStream(results, TypeInformation.of(Message.class));

        TableSchema schema = results.getSchema();
        System.out.println(schema.toString());
        System.out.println(schema.toRowDataType().toString());
        System.out.println(TypeInformation.of(Message.class).toString());

        outStream.print();

        JobTester.startTest(env);
        ms.sendRecord(TestUtils.createMessage(), 0);
        JobTester.stopTest();


    }
}
