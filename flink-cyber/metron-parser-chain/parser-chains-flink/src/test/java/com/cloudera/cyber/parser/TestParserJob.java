package com.cloudera.cyber.parser;

import com.cloudera.cyber.Message;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.CollectingSink;
import org.apache.flink.test.util.JobTester;
import org.apache.flink.test.util.ManualSource;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.TimeZone;

import static com.cloudera.parserchains.core.Constants.DEFAULT_INPUT_FIELD;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TestParserJob extends ParserJob {
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

    final String nameField = "Ada Lovelace";
    final String addressField = "1600 Pennsylvania Ave";
    final String phoneField = "212-555-1234";
    final long timestamp = Instant.now().toEpochMilli();
    final String timezone = TimeZone.getTimeZone("EST").getID();
    final String input = StringUtils.join(new String[] { nameField, addressField, phoneField, String.valueOf(timestamp), timezone }, ",");


    private ManualSource<MessageToParse> source;
    private CollectingSink<Message> sink = new CollectingSink<>();

    @Test
    public void testParser() throws Exception {
        ParameterTool params = ParameterTool.fromMap(new HashMap<String,String>() {{
            put(PARAM_CHAIN_CONFIG,chainWithRouting);
        }});

        StreamExecutionEnvironment env = createPipeline(params);

        JobTester.startTest(env);

        source.sendRecord(MessageToParse.builder().topic("test").originalSource(input).build());
        JobTester.stopTest();

        Message out = sink.poll(Duration.ofMillis(1000));
        assertThat("Output not null", out, notNullValue());
        assertThat("Original String is moved", out.getExtensions(), not(hasKey(DEFAULT_INPUT_FIELD)));
        assertThat("Timestamp is moved", out.getExtensions(), not(hasKey("timestamp")));

        // all other fields present and correct
        assertThat("name correct", out.getExtensions(), hasEntry(equalTo("name"), equalTo(nameField)));
    }

    @Override
    protected void writeResults(ParameterTool params, DataStream<Message> results) {
        results.addSink(sink);
    }

    @Override
    protected DataStream<MessageToParse> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        source = JobTester.createManualSource(env, TypeInformation.of(MessageToParse.class));
        return source.getDataStream();
    }
}
