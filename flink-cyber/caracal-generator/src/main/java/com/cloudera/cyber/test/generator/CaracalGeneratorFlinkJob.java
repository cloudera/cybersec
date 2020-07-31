package com.cloudera.cyber.test.generator;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.util.HashMap;
import java.util.Map;

public abstract class CaracalGeneratorFlinkJob {
    public static final String PARAMS_RECORDS_LIMIT = "generator.count";

    public StreamExecutionEnvironment createPipeline(ParameterTool params) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime);

        Map<GenerationSource, Double> outputs = new HashMap<>();
        outputs.put(new GenerationSource("Netflow/netflow_sample_1.json","netflow"), 2.0);
        outputs.put(new GenerationSource("Netflow/netflow_sample_2.json","netflow"), 4.0);
        outputs.put(new GenerationSource("Netflow/netflow_sample_3.json","netflow"), 1.0);

        outputs.put(new GenerationSource("DPI_Logs/Metadata_Module/http/http_sample_1.json","dpi_http"), 1.5);
        outputs.put(new GenerationSource("DPI_Logs/Metadata_Module/http/http_sample_2.json","dpi_http"), 1.0);
        outputs.put(new GenerationSource("DPI_Logs/Metadata_Module/http/http_sample_3.json","dpi_http"), 1.0);
        outputs.put(new GenerationSource("DPI_Logs/Metadata_Module/http/http_sample_4.json","dpi_http"), 1.0);


        SingleOutputStreamOperator<Tuple2<String, String>> generatedInput =
                env.addSource(new FreemarkerTemplateSource(outputs, params.getLong(PARAMS_RECORDS_LIMIT, -1))).name("Weighted Data Source");

        SingleOutputStreamOperator<Tuple2<String, Integer>> metrics = generatedInput
                .map(new MapFunction<Tuple2<String, String>, Tuple2<String, Integer>>() {
                    @Override
                    public Tuple2<String, Integer> map(Tuple2<String, String> stringStringTuple2) throws Exception {
                        return Tuple2.of(stringStringTuple2.f0, Integer.valueOf(1));
                    }
                })
                .keyBy(0)
                .window(TumblingProcessingTimeWindows.of(Time.seconds(30)))
                .sum(1);

        writeMetrics(params, metrics);
        writeResults(params, generatedInput);

        return env;
    }

    protected abstract void writeMetrics(ParameterTool params, SingleOutputStreamOperator<Tuple2<String, Integer>> metrics);

    protected abstract void writeResults(ParameterTool params, SingleOutputStreamOperator<Tuple2<String, String>> generatedInput);


}
