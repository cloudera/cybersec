package com.cloudera.cyber.caracal;

import com.cloudera.cyber.Message;
import org.apache.flink.util.Collector;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.cloudera.cyber.flink.Utils.getResourceAsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsMapContaining.hasKey;

public class SplittingFlatMapFunctionTest {
    private final static String testInput = getResourceAsString("dpi.json");

    @Test
    public void testSplittingWithHeader() throws Exception {
        List<Message> results = new ArrayList<>();

        SplittingFlatMapFunction splittingFlatMapFunction = new SplittingFlatMapFunction(SplitConfig.builder()
                .splitPath("$.http-stream['http.request'][*]")
                .headerPath("$.http-stream")
                .timestampField("start_ts")
                .timestampSource(SplittingFlatMapFunction.TimestampSource.HEADER)
                .timestampFunction("Math.round(parseFloat(ts)*1000,0)")
                .build());
        splittingFlatMapFunction.flatMap(testInput, new Collector<Message>() {
            @Override
            public void collect(Message message) {
                results.add(message);
                assertThat("Header fields present", message.getExtensions(), hasKey("start_ts"));
                assertThat("Part fields present", message.getExtensions(), hasKey("http.uri"));
            }

            @Override
            public void close() {

            }
        });

        assertThat("All splits returned", results, hasSize(4));
    }
}
