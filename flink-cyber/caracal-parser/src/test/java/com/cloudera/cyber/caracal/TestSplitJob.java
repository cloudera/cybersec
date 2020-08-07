package com.cloudera.cyber.caracal;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.parser.MessageToParse;
import com.cloudera.parserchains.core.utils.JSONUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.CollectingSink;
import org.apache.flink.test.util.JobTester;
import org.apache.flink.test.util.ManualSource;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static com.cloudera.cyber.flink.Utils.getResourceAsString;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsMapContaining.hasKey;

public class TestSplitJob extends SplitJob {
    ManualSource<MessageToParse> source;
    CollectingSink<Message> sink = new CollectingSink<Message>();

    @Test
    public void testSplitJob() throws Exception {
        ParameterTool params = ParameterTool.fromMap(new HashMap<String, String>(){{

        }});
        StreamExecutionEnvironment env = createPipeline(params);
        JobTester.startTest(env);

        Thread.sleep(1000);

        source.sendRecord(resourceToMessage("dpi_dns", "DPI_Logs/Metadata_Module/DNS/dns_sample_1.json"),100);
        source.sendRecord(resourceToMessage("dpi_dns", "DPI_Logs/Metadata_Module/DNS/dns_sample_2.json"),200);
        source.sendRecord(resourceToMessage("dpi_dns", "DPI_Logs/Metadata_Module/DNS/dns_sample_3.json"),300);

        source.sendRecord(resourceToMessage("dpi_http", "DPI_Logs/Metadata_Module/http/http_sample_1.json"),100);
        source.sendRecord(resourceToMessage("dpi_http", "DPI_Logs/Metadata_Module/http/http_sample_2.json"),200);
        source.sendRecord(resourceToMessage("dpi_http", "DPI_Logs/Metadata_Module/http/http_sample_3.json"),300);
        source.sendRecord(resourceToMessage("dpi_http", "DPI_Logs/Metadata_Module/http/http_sample_4.json"),400);

        JobTester.stopTest();

        int expectedMessages = 23;

        List<Message> results = new ArrayList<>();
        for (int i = 0; i < expectedMessages; i++) {
            try {
                results.add(sink.poll(Duration.ofMillis(100)));
            } catch (TimeoutException e ){
                break;
            }
        }

        assertThat("All results found", results, hasSize(expectedMessages));

        results.stream().forEach(m -> {
            assertThat("Message has timestamp", m.getTs().getMillis(), allOf(notNullValue(), greaterThan(10000000L)));
            assertThat("Message has source", m.getExtensions(), hasKey("source"));
        });

    }

    private MessageToParse resourceToMessage(String topic, String file) {
        return MessageToParse.newBuilder()
                .setOriginalSource(getResourceAsString(file))
                .setTopic(topic)
                .setOffset(0)
                .setPartition(0)
                .build();
    }

    @Override
    protected DataStream<SplitConfig> createConfigSource(StreamExecutionEnvironment env, ParameterTool params) {
        try {
            return env.fromCollection(parseConfig());
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected List<SplitConfig> parseConfig() throws IllegalArgumentException, IOException {
        return JSONUtils.INSTANCE.getMapper().readValue(getResourceAsString("splits.json"), new TypeReference<List<SplitConfig>>() {});
    }

    @Override
    protected void writeResults(ParameterTool params, DataStream<Message> results) {
        results.addSink(sink);
    }

    @Override
    protected void writeOriginalsResults(ParameterTool params, DataStream<MessageToParse> results) {

    }

    @Override
    protected DataStream<MessageToParse> createSource(StreamExecutionEnvironment env, ParameterTool params, Iterable<String> topics) {
        source =  JobTester.createManualSource(env, TypeInformation.of(MessageToParse.class));
        return source.getDataStream();
    }
}
