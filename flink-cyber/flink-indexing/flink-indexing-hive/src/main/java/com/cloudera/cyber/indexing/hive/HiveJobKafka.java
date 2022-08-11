package com.cloudera.cyber.indexing.hive;

import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.Utils;
import com.cloudera.cyber.scoring.ScoredMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Preconditions;

import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_INPUT;

@Slf4j
public class HiveJobKafka extends HiveJob {

    private static final String PARAMS_GROUP_ID = "kafka.group.id";
    private static final String DEFAULT_GROUP_ID = "indexer-hive";

    public static void main(String[] args) throws Exception {
        ParameterTool params = Utils.getParamToolsFromProperties(args);
        FlinkUtils.executeEnv(new HiveJobKafka().createPipeline(params),"Indexing - Hive", params);
    }

    @Override
    protected SingleOutputStreamOperator<ScoredMessage> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.addSource(
                new FlinkUtils(ScoredMessage.class).createKafkaGenericSource(params.getRequired(PARAMS_TOPIC_INPUT), params, params.get(PARAMS_GROUP_ID, DEFAULT_GROUP_ID))
        ).name("Kafka Source").uid("kafka-source");
    }
}
