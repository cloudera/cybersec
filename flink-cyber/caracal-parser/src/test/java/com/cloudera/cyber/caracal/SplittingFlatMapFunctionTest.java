/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

package com.cloudera.cyber.caracal;

import static com.cloudera.cyber.flink.Utils.getResourceAsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsMapContaining.hasKey;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.TestUtils;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;
import org.junit.Test;

public class SplittingFlatMapFunctionTest {
    private static final String testInput = getResourceAsString("dpi.json");

    @Test
    public void testSplittingWithHeader() throws Exception {
        List<Message> results = new ArrayList<>();

        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA", "SunRsaSign");
        gen.initialize(1024, new SecureRandom());
        KeyPair pair = gen.generateKeyPair();

        SplittingFlatMapFunction splittingFlatMapFunction = new SplittingFlatMapFunction(SplitConfig.builder()
                .splitPath("$.http-stream['http.request'][*]")
                .headerPath("$.http-stream")
                .timestampField("start_ts")
                .timestampSource(SplittingFlatMapFunction.TimestampSource.HEADER)
                .timestampFunction("Math.round(parseFloat(ts)*1000,0)")
                .build(), pair.getPrivate());

        splittingFlatMapFunction.open(new Configuration());

        splittingFlatMapFunction.flatMap(
                TestUtils.createMessageToParse(testInput).topic("test").build(),
                new Collector<Message>() {
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
