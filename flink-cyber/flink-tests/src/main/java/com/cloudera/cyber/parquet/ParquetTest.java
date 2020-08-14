package com.cloudera.cyber.parquet;

import com.cloudera.cyber.parser.MessageToParse;
import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.core.fs.Path;
import org.apache.flink.formats.parquet.avro.ParquetAvroWriters;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class ParquetTest {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.enableCheckpointing(100);

        DataStreamSource<MessageToParse> source = env.fromCollection(createMessages());
        StreamingFileSink<MessageToParse> sink = createSink();

        source.addSink(sink);

        JobExecutionResult out = env.execute("test");

        Thread.sleep(10000);
        System.out.println(out.toString());
    }


    private static StreamingFileSink<MessageToParse> createSink() {

        // write the original sources to HDFS files
        File folder =new File("/Users/simonellistonball/tmp/testoutput");

        return StreamingFileSink.forBulkFormat(
                Path.fromLocalFile(folder),
                ParquetAvroWriters.forSpecificRecord(MessageToParse.class))
                .build();
    }

    private static List<MessageToParse> createMessages() {
        return Arrays.asList(MessageToParse.newBuilder()
                .setOffset(0)
                .setPartition(0)
                .setTopic("test")
                .setOriginalSource("original")
                .build(),
                MessageToParse.newBuilder()
                        .setOffset(1)
                        .setPartition(0)
                        .setTopic("test2")
                        .setOriginalSource("original2")
                        .build()
        );
    }
}
