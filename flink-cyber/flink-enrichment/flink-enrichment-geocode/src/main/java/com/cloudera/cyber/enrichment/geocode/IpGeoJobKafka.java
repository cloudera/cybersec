package com.cloudera.cyber.enrichment.geocode;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.Utils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;

import java.util.Arrays;
import java.util.List;

public class IpGeoJobKafka extends IpGeoJob {
    public static void main(String[] args) throws Exception {
        ParameterTool params = Utils.getParamToolsFromProperties(args);
        new IpGeoJobKafka()
                .createPipeline(params)
                .execute("Flink IP Geocode");
    }

    @Override
    protected void writeResults(ParameterTool params, DataStream<Message> results) {
        FlinkKafkaProducer<Message> sink = new FlinkUtils<>(Message.class).createKafkaSink(
                params.getRequired("topic.output"),
                "enrichment-geocode",
                params);
        results.addSink(sink).name("Kafka Results").uid("kafka.results");
    }

    /**
     * Returns a consumer group id for the geocoder ensuring that each topic is only processed once with the same fields
     *
     * @param inputTopic topic to read from
     * @param ipFields the ip fields to be geocoded
     * @return Kafka group id for geocoder
     */
    private String createGroupId(String inputTopic, List<String> ipFields) {
        List<String> parts = Arrays.asList("ipgeo",
                inputTopic,
                String.valueOf(ipFields.hashCode()));
        return String.join(".", parts);
    }

    @Override
    protected SingleOutputStreamOperator<Message> createSource(StreamExecutionEnvironment env, ParameterTool params, List<String> ipFields) {
        String inputTopic = params.getRequired("topic.input");
      return env.addSource(FlinkUtils.createKafkaSource(inputTopic,
                params,createGroupId(inputTopic, ipFields)))
                .name("Kafka Source")
                .uid("kafka.input");
    }

}
