package com.cloudera.cyber.indexer;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.core.fs.Path;
import org.apache.flink.formats.parquet.avro.ParquetAvroWriters;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink;
import org.apache.flink.util.Preconditions;

import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_INPUT;

public class ParquetJobKafka extends ParquetJob {

    public static void main(String[] args) throws Exception {
        Preconditions.checkArgument(args.length == 1, "Arguments must consist of a single properties file");
        new ParquetJobKafka().createPipeline(ParameterTool.fromArgs(args)).execute("Indexing - Parquet");
    }

    @Override
    public DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.addSource(
                new FlinkUtils(Message.class).createKafkaSource(params.getRequired(PARAMS_TOPIC_INPUT), params, "indexer-parquet")
        );
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
