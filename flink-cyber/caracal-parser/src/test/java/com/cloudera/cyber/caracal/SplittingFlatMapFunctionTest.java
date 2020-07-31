package com.cloudera.cyber.caracal;

import com.cloudera.cyber.Message;
import org.apache.flink.util.Collector;
import org.junit.Test;

import java.io.IOException;
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

        SplittingFlatMapFunction splittingFlatMapFunction = new SplittingFlatMapFunction("$.http-stream['http.request'][*]", "$.http-stream", "start_ts", SplittingFlatMapFunction.TimestampSource.HEADER);
        splittingFlatMapFunction.flatMap(testInput, new Collector<Message>() {
            @Override
            public void collect(Message message) {
                results.add(message);
                assertThat("Header fields present", message.getFields(), hasKey("start_ts"));
                assertThat("Part fields present", message.getFields(), hasKey("http.uri"));
            }

            @Override
            public void close() {

            }
        });

        assertThat("All splits returned", results, hasSize(4));

    }
}
