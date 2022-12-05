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

package com.cloudera.cyber.parser;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.TestUtils;
import com.google.common.io.Resources;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.JobTester;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.stream.Stream;

import static com.cloudera.parserchains.core.Constants.DEFAULT_INPUT_FIELD;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

public class TestParserJobChainDirectory extends AbstractParserJobTest {

    public static final String CHAIN_DIR_ERROR = "chain-dir/error";
    public static final String CHAIN_DIR_SUCCESS = "chain-dir/success";
    public static final String CHAIN_DIR_DUPLICATE = "chain-dir/duplicate";

    /**
     * {"store":
     * {"book":[
     * {"category":"reference",
     * "author":"	    Nigel \"Rees\", Stas 	",
     * "title":"Sayings of the Century",
     * "price":8.95},
     * {"category":"fiction",
     * "author":"Evelyn Waugh",
     * "title":"Sword of Honour",
     * "price":12.99},
     * {"category":"fiction",
     * "author":"Herman Melville",
     * "title":"Moby Dick",
     * "isbn":"0-553-21311-3",
     * "price":8.99},
     * {"category":"fiction",
     * "author":"J. R. R. Tolkien",
     * "title":"The Lord of the Rings",
     * "isbn":"0-395-19395-8",
     * "price":22.99}],
     * "bicycle":
     * {"color":"red",
     * "price":19.95}},
     * "expensive":10}
     */
    @Multiline
    private String givenInput;

    public static Stream<Arguments> mutuallyExclusiveParams() {
        return Stream.of(
                Arguments.of(getParamToolWithEmptyEntries(PARAM_CHAIN_CONFIG, PARAM_CHAIN_CONFIG_DIRECTORY)),
                Arguments.of(getParamToolWithEmptyEntries(PARAM_CHAIN_CONFIG, PARAM_CHAIN_CONFIG_FILE)),
                Arguments.of(getParamToolWithEmptyEntries(PARAM_CHAIN_CONFIG_FILE, PARAM_CHAIN_CONFIG_DIRECTORY)),
                Arguments.of(getParamToolWithEmptyEntries(PARAM_CHAIN_CONFIG, PARAM_CHAIN_CONFIG_FILE, PARAM_CHAIN_CONFIG_DIRECTORY)));
    }

    public static Stream<Arguments> parserExceptionParams() {
        return Stream.of(
                Arguments.of(Tuple2.of(CHAIN_DIR_ERROR,
                        String.format("Wasn't able to read the chain file [file:%s/test-config1.json]!",
                                Resources.getResource(CHAIN_DIR_ERROR).getPath()))),
                Arguments.of(Tuple2.of(CHAIN_DIR_DUPLICATE,
                        String.format("Found a duplicate schema named [%s] in the [file:%s/test-config2.json] file, which isn't allowed!",
                                "test", Resources.getResource(CHAIN_DIR_DUPLICATE).getPath()))));
    }

    @Test
    public void testParser() throws Exception {
        ParameterTool params = ParameterTool.fromMap(new HashMap<String, String>() {{
            put(PARAM_CHAIN_CONFIG_DIRECTORY, Resources.getResource(CHAIN_DIR_SUCCESS).getPath());
            put(PARAM_PRIVATE_KEY, getKeyBase64());
        }});

        StreamExecutionEnvironment env = createPipeline(params);

        JobTester.startTest(env);
        source.sendRecord(TestUtils.createMessageToParse(givenInput).build());
        JobTester.stopTest();

        Message out = sink.poll();
        assertThat("Output not null", out, notNullValue());
        assertThat("Original String is moved", out.getExtensions(), not(hasKey(DEFAULT_INPUT_FIELD)));
        assertThat("Timestamp is moved", out.getExtensions(), not(hasKey("timestamp")));

        // all other fields present and correct
        assertThat("name correct", out.getExtensions(),
                hasEntry(equalTo("timestamp_new"), Matchers.any(String.class)));
    }

    @Test
    public void testParserFail() throws Exception {
        final String path = Resources.getResource(CHAIN_DIR_SUCCESS).getPath() + "-not-existing";
        ParameterTool params = ParameterTool.fromMap(new HashMap<String, String>() {{
            put(PARAM_CHAIN_CONFIG_DIRECTORY, path);
            put(PARAM_PRIVATE_KEY, getKeyBase64());
        }});

        try {
            createPipeline(params);
        } catch (RuntimeException e) {
            final String message = String.format("Provided config directory doesn't exist or empty [%s]!", path);
            assertThat("Exception message not expected", e.getMessage(), is(message));
            assertThat("Exception type not expected", e.getClass(), is(RuntimeException.class));
        }
    }

    @ParameterizedTest
    @MethodSource("mutuallyExclusiveParams")
    public void testParserMutuallyExclusiveConfig(ParameterTool params) throws Exception {
        final String message = "It's not allowed to provide more than one chain param! " +
                "Select one of the following: " + PARAM_CHAIN_CONFIG_EXCLUSIVE_LIST;

        try {
            createPipeline(params);
        } catch (RuntimeException e) {
            assertThat("Exception message not expected", e.getMessage(), is(message));
            assertThat("Exception type not expected", e.getClass(), is(RuntimeException.class));
        }
    }

    @ParameterizedTest
    @MethodSource("parserExceptionParams")
    public void testParserExceptions(Tuple2<String, String> excParams) throws Exception {
        ParameterTool params = ParameterTool.fromMap(new HashMap<String, String>() {{
            put(PARAM_CHAIN_CONFIG_DIRECTORY, Resources.getResource(excParams.f0).getPath());
            put(PARAM_PRIVATE_KEY, getKeyBase64());
        }});

        try {
            createPipeline(params);
        } catch (RuntimeException e) {
            assertThat("Exception message not expected", e.getMessage(), is(excParams.f1));
            assertThat("Exception type not expected", e.getClass(), is(RuntimeException.class));
        }
    }

    static ParameterTool getParamToolWithEmptyEntries(String... entryKeys) {
        return ParameterTool.fromMap(new HashMap<String, String>() {{
            for (String key : entryKeys) {
                put(key, "");
            }
        }});
    }
}
