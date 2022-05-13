package com.cloudera.cyber.parser;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.TestUtils;
import com.cloudera.cyber.commands.CommandType;
import com.cloudera.cyber.commands.EnrichmentCommand;
import com.google.common.collect.ImmutableMap;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.JobTester;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import static com.cloudera.parserchains.core.Constants.DEFAULT_INPUT_FIELD;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TestTopicMapParserJob extends AbstractParserJobTest {
    /**
     * { "test": {
     * "id" : "3b31e549-340f-47ce-8a71-d702685137f4",
     * "name" : "My Parser Chain",
     * "parsers" : [ {
     * "id" : "26bf648f-930e-44bf-a4de-bfd34ac16165",
     * "name" : "Delimited Text",
     * "type" : "com.cloudera.parserchains.parsers.DelimitedTextParser",
     * "config" : {
     * "inputField" : [ {
     * "inputField": "original_string"
     * }],
     * "outputField" : [ {
     * "fieldIndex" : "0",
     * "fieldName" : "name"
     * }, {
     * "fieldIndex" : "1",
     * "fieldName" : "address"
     * }, {
     * "fieldIndex" : "2",
     * "fieldName" : "phone"
     * }, {
     * "fieldIndex" : "3",
     * "fieldName" : "timestamp"
     * }, {
     * "fieldIndex": "4",
     * "fieldName": "timezone"
     * }  ]
     * }
     * }, {
     * "id" : "123e4567-e89b-12d3-a456-556642440000",
     * "name" : "Router",
     * "type" : "Router",
     * "config" : { },
     * "routing" : {
     * "matchingField" : "name",
     * "routes" : [ {
     * "matchingValue" : "Ada Lovelace",
     * "default" : false,
     * "subchain" : {
     * "id" : "3b31e549-340f-47ce-8a71-d702685137f4",
     * "name" : "Success Chain",
     * "parsers" : [ {
     * "id" : "123e4567-e89b-12d3-a456-556642440000",
     * "name" : "Timestamp",
     * "type" : "com.cloudera.parserchains.parsers.TimestampParser",
     * "config" : {
     * "outputField" : [ {
     * "outputField" : "processing_time"
     * } ]
     * }
     * } ]
     * }
     * }, {
     * "matchingValue" : "",
     * "default" : true,
     * "subchain" : {
     * "id" : "cdb0729f-a929-4f3c-9cb7-675b57d10a73",
     * "name" : "Default Chain",
     * "parsers" : [ {
     * "id" : "ceb95dd5-1e3f-41f2-bf60-ee2fe2c962c6",
     * "name" : "Error",
     * "type" : "com.cloudera.parserchains.parsers.AlwaysFailParser",
     * "config" : { }
     * } ]
     * }
     * } ]
     * }
     * } ]
     * }
     * }
     */
    @Multiline
    private String chainWithRouting;

    private final String topicMap = "{\"in.*\" : {\"chainKey\": \"test\", \"source\" : \"test_source\"}, \"exact\" : {\"chainKey\": \"test\", \"source\" : \"test_source\"}}";
    private final String streamingTopicMap = "{  \"nostream\" : {\"chainKey\": \"test\", \"source\" : \"nostream\"}, \"stream\" : {\"chainKey\": \"test\", \"source\" : \"streaming\"}, \"badstream\" : {\"chainKey\": \"test\", \"source\" : \"badstreaming\"}}";

    final String nameField = "Ada Lovelace";
    final String addressField = "1600 Pennsylvania Ave";
    final String phoneField = "212-555-1234";
    final long timestamp = Instant.now().toEpochMilli();
    final String timezone = TimeZone.getTimeZone("EST").getID();
    final String input = StringUtils.join(new String[]{nameField, addressField, phoneField, String.valueOf(timestamp), timezone}, ",");
    final Map<String, String> expectedEnrichmentValues = ImmutableMap.of("address", addressField);

    @Test
    public void testParser() throws Exception {

        ParameterTool params = ParameterTool.fromMap(new HashMap<String, String>() {{
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

        Map<String, String> expectedTopicToSource = ImmutableMap.of("input", "test_source",
                                                                    "test", "test",
                                                                    "exact", "test_source");
        IntStream.range(0, 3).forEach(i -> {
            try {
                verifyParsedMessage(expectedTopicToSource);
            } catch (TimeoutException e) {
                Assert.fail(String.format("Timeout exception for message %d", i));
            }
        });

    }

    @Test
    public void testStreamingEnrichment() throws Exception {

        File streamingEnrichmentsFile = new File("src/test/resources/streaming_enrichments.json");

        ParameterTool params = ParameterTool.fromMap(new HashMap<String, String>() {{
            put(PARAM_CHAIN_CONFIG, chainWithRouting);
            put(PARAM_TOPIC_MAP_CONFIG, streamingTopicMap);
            put(PARAM_PRIVATE_KEY, getKeyBase64());
            put(PARAM_STREAMING_ENRICHMENTS_CONFIG, streamingEnrichmentsFile.getAbsolutePath());
        }});

        StreamExecutionEnvironment env = createPipeline(params);

        JobTester.startTest(env);
        source.sendRecord(TestUtils.createMessageToParse(input, "stream").build());
        source.sendRecord(TestUtils.createMessageToParse(input, "stream").build());
        source.sendRecord(TestUtils.createMessageToParse(input, "stream").build());
        source.sendRecord(TestUtils.createMessageToParse(input, "nostream").build());
        source.sendRecord(TestUtils.createMessageToParse(input, "badstream").build());

        JobTester.stopTest();

        Map<String, String>  expectedTopicToSource = ImmutableMap.of("stream", "streaming",
                                                                     "nostream", "nostream",
                                                                      "badstream", "badstreaming");
        IntStream.range(0, 5).forEach(i -> {
            try {
                verifyParsedMessage(expectedTopicToSource);
            } catch (TimeoutException e) {
                Assert.fail(String.format("Timeout exception for message %d", i));
            }
        });

        IntStream.range(0, 3).forEach(i -> {
            try {
                verifyEnrichmentCommand();
            } catch (TimeoutException e) {
                Assert.fail(String.format("Timeout exception for message %d", i));
            }
        });

        verifyErrorMessage();

        Assert.assertTrue(enrichmentCommandSink.isEmpty());

    }

    void verifyEnrichmentCommand()throws TimeoutException{
        EnrichmentCommand enrichmentCommand = enrichmentCommandSink.poll();
        assertThat("Enrichment not null", enrichmentCommand, notNullValue());
        assertThat("Enrichment command type correct", enrichmentCommand.getType(), equalTo(CommandType.ADD));
        assertThat("Enrichment type correct", enrichmentCommand.getPayload().getType(), equalTo("address"));
        assertThat("Enrichment key correct", enrichmentCommand.getPayload().getKey(), equalTo(nameField));

        assertThat("Enrichment values correct", enrichmentCommand.getPayload().getEntries(), equalTo(expectedEnrichmentValues));
    }

    void verifyErrorMessage() throws TimeoutException{
        Message errorMessage = errorSink.poll();
        assertThat("Error message must have at least one data quality message", errorMessage.getDataQualityMessages().size(), greaterThan(0));
    }

    void verifyParsedMessage(Map<String, String> expectedTopicToSource) throws TimeoutException {
        Message message = sink.poll();
        assertThat("Output not null", message, notNullValue());
        assertThat("Original String is moved", message.getExtensions(), not(hasKey(DEFAULT_INPUT_FIELD)));
        assertThat("Timestamp is moved", message.getExtensions(), not(hasKey("timestamp")));

        // all other fields present and correct
        assertThat("name correct", message.getExtensions(), hasEntry(equalTo("name"), equalTo(nameField)));

        assertThat("source should map to test", message.getSource(), equalTo(expectedTopicToSource.get(message.getOriginalSource().getTopic())));
    }
}
