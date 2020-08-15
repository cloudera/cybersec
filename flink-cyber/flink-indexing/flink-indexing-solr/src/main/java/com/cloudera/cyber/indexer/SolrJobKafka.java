package com.cloudera.cyber.indexer;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Preconditions;

import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_INPUT;

public class SolrJobKafka extends SolrJob {

    public static void main(String[] args) throws Exception {
        Preconditions.checkArgument(args.length == 1, "Arguments must consist of a single properties file");
        new SolrJobKafka().createPipeline(ParameterTool.fromArgs(args)).execute("Indexing - Solr");
    }


    @Override
    public DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.addSource(
                new FlinkUtils(Message.class).createKafkaSource(params.getRequired(PARAMS_TOPIC_INPUT), params, "indexer-solr")
        );
    }

    @Override
    protected void writeResults(DataStream<Message> results, ParameterTool params) {
        throw new IllegalArgumentException("Solr not yet implemented");
    }
}
