package com.cloudera.cyber.parser;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.TestUtils;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.JobTester;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import static com.cloudera.parserchains.core.Constants.DEFAULT_INPUT_FIELD;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.equalTo;

public class TestTopicMapParserJob extends AbstractParserJobTest {
        /**
         * { "test": {
         *   "id" : "3b31e549-340f-47ce-8a71-d702685137f4",
         *   "name" : "My Parser Chain",
         *   "parsers" : [ {
         *     "id" : "26bf648f-930e-44bf-a4de-bfd34ac16165",
         *     "name" : "Delimited Text",
         *     "type" : "com.cloudera.parserchains.parsers.DelimitedTextParser",
         *     "config" : {
         *       "inputField" : [ {
         *         "inputField": "original_string"
         *       }],
         *       "outputField" : [ {
         *         "fieldIndex" : "0",
         *         "fieldName" : "name"
         *       }, {
         *         "fieldIndex" : "1",
         *         "fieldName" : "address"
         *       }, {
         *         "fieldIndex" : "2",
         *         "fieldName" : "phone"
         *       }, {
         *         "fieldIndex" : "3",
         *         "fieldName" : "timestamp"
         *       }, {
         *         "fieldIndex": "4",
         *         "fieldName": "timezone"
         *       }  ]
         *     }
         *   }, {
         *     "id" : "123e4567-e89b-12d3-a456-556642440000",
         *     "name" : "Router",
         *     "type" : "Router",
         *     "config" : { },
         *     "routing" : {
         *       "matchingField" : "name",
         *       "routes" : [ {
         *         "matchingValue" : "Ada Lovelace",
         *         "default" : false,
         *         "subchain" : {
         *           "id" : "3b31e549-340f-47ce-8a71-d702685137f4",
         *           "name" : "Success Chain",
         *           "parsers" : [ {
         *             "id" : "123e4567-e89b-12d3-a456-556642440000",
         *             "name" : "Timestamp",
         *             "type" : "com.cloudera.parserchains.parsers.TimestampParser",
         *             "config" : {
         *               "outputField" : [ {
         *                 "outputField" : "processing_time"
         *               } ]
         *             }
         *           } ]
         *         }
         *       }, {
         *         "matchingValue" : "",
         *         "default" : true,
         *         "subchain" : {
         *           "id" : "cdb0729f-a929-4f3c-9cb7-675b57d10a73",
         *           "name" : "Default Chain",
         *           "parsers" : [ {
         *             "id" : "ceb95dd5-1e3f-41f2-bf60-ee2fe2c962c6",
         *             "name" : "Error",
         *             "type" : "com.cloudera.parserchains.parsers.AlwaysFailParser",
         *             "config" : { }
         *           } ]
         *         }
         *       } ]
         *     }
         *   } ]
         * }
         * }
         */
        @Multiline
        private String chainWithRouting;

        private final String topicMap = "{\"in.*\" : {\"chainKey\": \"test\", \"source\" : \"test_source\"}, \"exact\" : {\"chainKey\": \"test\", \"source\" : \"test_source\"}}";

        final String nameField = "Ada Lovelace";
        final String addressField = "1600 Pennsylvania Ave";
        final String phoneField = "212-555-1234";
        final long timestamp = Instant.now().toEpochMilli();
        final String timezone = TimeZone.getTimeZone("EST").getID();
        final String input = StringUtils.join(new String[] { nameField, addressField, phoneField, String.valueOf(timestamp), timezone }, ",");

        @Override
        @Test
        public void testParser() throws Exception {
            ParameterTool params = ParameterTool.fromMap(new HashMap<String,String>() {{
                put(PARAM_CHAIN_CONFIG, chainWithRouting);
                put(PARAM_TOPIC_MAP_CONFIG, topicMap);
                put(PARAM_PRIVATE_KEY, getKeyBase64());
            }});

            StreamExecutionEnvironment env = createPipeline(params);

            JobTester.startTest(env);
            source.sendRecord(TestUtils.createMessageToParse(input, "input").build());
            source.sendRecord(TestUtils.createMessageToParse(input, "test").build());
            source.sendRecord(TestUtils.createMessageToParse(input, "exact").build());
            JobTester.stopTest();
            IntStream.range(0, 3).forEach( i -> {
                try {
                    verifyParsedMessage();
                } catch (TimeoutException e) {
                    Assert.fail(String.format("Timeout exception for message %d", i));
                }
            });
        }

        void verifyParsedMessage() throws TimeoutException {
            Message message = sink.poll();
            assertThat("Output not null", message, notNullValue());
            assertThat("Original String is moved", message.getExtensions(), not(hasKey(DEFAULT_INPUT_FIELD)));
            assertThat("Timestamp is moved", message.getExtensions(), not(hasKey("timestamp")));

            // all other fields present and correct
            assertThat("name correct", message.getExtensions(), hasEntry(equalTo("name"), equalTo(nameField)));
            String expectedSource = "test_source";
            if (message.getOriginalSource().getTopic().equals("test")) {
                expectedSource = "test";
            }
            assertThat("source should map to test", message.getSource(), equalTo(expectedSource));

        }
    }
