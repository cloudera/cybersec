package com.cloudera.cyber.parser;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.TestUtils;
import com.google.common.io.Resources;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.JobTester;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;

import static com.cloudera.parserchains.core.Constants.DEFAULT_INPUT_FIELD;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TestNetflowBParser extends AbstractParserJobTest {
    private static final String config = "{\n" +
            "    \"netflow\": {\n" +
            "        \"id\": \"220ee8c5-07d7-48d9-8df5-7d23376cb664\",\n" +
            "        \"name\": \"Netflow Parser\",\n" +
            "        \"parsers\": [\n" +
            "            {\n" +
            "                \"id\": \"f812c6dc-40cc-4c77-abf8-e15fccdfea32\",\n" +
            "                \"name\": \"Netflow as JSON\",\n" +
            "                \"type\": \"com.cloudera.parserchains.parsers.JSONParser\",\n" +
            "                \"config\": {\n" +
            "                    \"input\": {\n" +
            "                        \"input\": \"original_string\"\n" +
            "                    },\n" +
            "                    \"norm\": {\n" +
            "                        \"norm\": \"UNFOLD_NESTED\"\n" +
            "                    }\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"6b8797a2-95df-4021-83c2-60ac4c786e67\",\n" +
            "                \"name\": \"Field Renamer\",\n" +
            "                \"type\": \"com.cloudera.parserchains.parsers.RenameFieldParser\",\n" +
            "                \"config\": {\n" +
            "                    \"fieldToRename\": [\n" +
            "                        {\n" +
            "                            \"from\": \"timestamp_end\",\n" +
            "                            \"to\": \"timestamp\"\n" +
            "                        }\n" +
            "                    ]\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"9549004f-83e4-4d24-8baa-abdbdad06e61\",\n" +
            "                \"name\": \"Timestamp Parser\",\n" +
            "                \"type\": \"com.cloudera.parserchains.parsers.TimestampFormatParser\",\n" +
            "                \"config\": {\n" +
            "                    \"fields\": [\n" +
            "                        {\n" +
            "                            \"field\": \"timestamp\",\n" +
            "                            \"format\": \"yyyy-MM-dd HH:mm:ss.SSSSSS\",\n" +
            "                            \"tz\": \"UTC\"\n" +
            "                        }\n" +
            "                    ]\n" +
            "                }\n" +
            "            }\n" +
            "        ]\n" +
            "    }\n" +
            "}\n";


    @Test
    public void testParser() throws Exception {
        ParameterTool params = ParameterTool.fromMap(new HashMap<String, String>() {{
            put(PARAM_CHAIN_CONFIG, config);
            put(PARAM_PRIVATE_KEY, getKeyBase64());
        }});

        StreamExecutionEnvironment env = createPipeline(params);

        JobTester.startTest(env);

        // happy path message with configuration and parses correctly
        sendRecord("netflow", "netflow_b.json");
        // send a parser error
        sendRecord("netflow", "netflow_berror.json");
        // send a message with no configuration
        sendRecord("netflow_b", "netflow_b.json");

        JobTester.stopTest();


        Message out = sink.poll(Duration.ofMillis(1000));
        assertThat("Output not null", out, notNullValue());
        assertThat("Original String is moved", out.getExtensions(), not(hasKey(DEFAULT_INPUT_FIELD)));
        assertThat("Timestamp is moved", out.getExtensions(), not(hasKey("timestamp")));

        Message error = errorSink.poll(Duration.ofMillis(1000));
        assertThat("Error output not null", error, notNullValue());
        assertThat("Original String is preserved", error.getExtensions(), hasKey(DEFAULT_INPUT_FIELD));
        assertThat("data quality message reported", error.getDataQualityMessages(), hasSize(1));

        error = errorSink.poll(Duration.ofMillis(1000));
        assertThat("Error output not null", error, notNullValue());
        assertThat("Original String is preserved", error.getExtensions(), hasKey(DEFAULT_INPUT_FIELD));
        assertThat("data quality message reported", error.getDataQualityMessages(), hasSize(1));
    }

    private void sendRecord(String topic, String resourceName) throws IOException {
        source.sendRecord(TestUtils
                .createMessageToParse(
                        Resources.toString(Resources.getResource(resourceName), StandardCharsets.UTF_8)
                )
                .topic(topic)
                .build());
    }
}
