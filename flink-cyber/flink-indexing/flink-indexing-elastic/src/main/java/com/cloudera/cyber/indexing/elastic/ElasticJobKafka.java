package com.cloudera.cyber.indexing.elastic;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.indexing.MessageRetry;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Preconditions;

import java.io.IOException;

import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_INPUT;

public class ElasticJobKafka extends ElasticJob {
    private static final String PARAMS_RETRY_TOPIC = "retry.topic";

    public static void main(String[] args) throws Exception {
        Preconditions.checkArgument(args.length == 1, "Arguments must consist of a single properties file");
        ParameterTool params = ParameterTool.fromPropertiesFile(args[0]);
        new ElasticJobKafka().createPipeline(params).execute("Indexing - Parquet");
    }

    @Override
    protected DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.addSource(
                new FlinkUtils(Message.class).createKafkaSource(params.getRequired(PARAMS_TOPIC_INPUT), params, "indexer-parquet")
        ).name("Kafka Source").uid("kafka-source");
    }

    @Override
    protected DataStream<MessageRetry> createRetrySource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.addSource(
                new FlinkUtils(MessageRetry.class).createKafkaGenericSource(params.getRequired(PARAMS_RETRY_TOPIC), params, "indexer-solr"));
    }

    @Override
    protected void writeRetryResults(DataStream<MessageRetry> retries, ParameterTool params) throws IOException {
        retries.addSink(new FlinkUtils<>(MessageRetry.class).createKafkaSink(params.getRequired(PARAMS_RETRY_TOPIC), params));
    }

}
