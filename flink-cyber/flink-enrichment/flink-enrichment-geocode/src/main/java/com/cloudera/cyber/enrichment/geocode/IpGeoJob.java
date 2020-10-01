package com.cloudera.cyber.enrichment.geocode;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.Arrays;
import java.util.List;

public abstract class IpGeoJob {
    public static final String PARAM_GEO_FIELDS = "geo.ip_fields";
    public static final String PARAM_GEO_DATABASE_PATH = "geo.database_path";

    protected StreamExecutionEnvironment createPipeline(ParameterTool params) {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        FlinkUtils.setupEnv(env, params);
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        List<String> ipFields = Arrays.asList(params.getRequired(PARAM_GEO_FIELDS).split(","));
        String geoDatabasePath = params.getRequired(PARAM_GEO_DATABASE_PATH);

        DataStream<Message> source = createSource(env, params, ipFields);

        SingleOutputStreamOperator<Message> results = IpGeo.geo(source, ipFields, geoDatabasePath);
        writeResults(params, results);

        return env;
    }

    protected abstract void writeResults(ParameterTool params, DataStream<Message> results);

    protected abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params, List<String> ipFields);

}