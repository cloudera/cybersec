package com.cloudera.cyber.indexer;

import com.cloudera.cyber.Message;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.core.fs.Path;
import org.apache.flink.formats.parquet.avro.ParquetAvroWriters;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink;

public class ParquetJobKafka extends ParquetJob {

    @Override
    protected DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        return null;
    }

    @Override
    protected void writeResults(DataStream<Message> results, ParameterTool params) {
        final StreamingFileSink<Message> sink = StreamingFileSink
                .forBulkFormat(new Path(params.getRequired("output.path")),
                        ParquetAvroWriters.forSpecificRecord(Message.class))
                .build();
        results.addSink(sink);
    }
}
