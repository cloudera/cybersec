package com.cloudera.cyber.test;

import com.cloudera.cyber.test.generator.CaracalGeneratorFlinkJob;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.test.util.CollectingSink;
import org.apache.flink.test.util.JobTester;
import org.junit.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class TestCaracalGeneratorJob extends CaracalGeneratorFlinkJob {
    private CollectingSink<Tuple2<String, String>> sink = new CollectingSink<>();
    private CollectingSink<Tuple2<String, Integer>> metricSink = new CollectingSink<>();

    @Test
    public void testGenerator() throws Exception {
        int count = 100;
        JobTester.startTest(createPipeline(ParameterTool.fromMap(new HashMap<String, String>() {{
            put(PARAMS_RECORDS_LIMIT, String.valueOf(count));
        }})));

        JobTester.stopTest();

        List<Tuple2<String, String>> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            results.add(sink.poll(Duration.ofMillis(100)));
        }

        assertThat("Has generated results", results, hasSize(count));
    }

    @Override
    protected void writeMetrics(ParameterTool params, SingleOutputStreamOperator<Tuple2<String, Integer>> metrics) {
        metrics.addSink(metricSink);
    }

    @Override
    protected void writeResults(ParameterTool params, SingleOutputStreamOperator<Tuple2<String, String>> generatedInput) {
        generatedInput.addSink(sink);
    }
}
