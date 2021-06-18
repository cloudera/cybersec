package com.cloudera.cyber;

import com.cloudera.cyber.commands.CommandType;
import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.commands.EnrichmentCommandResponse;
import com.cloudera.cyber.parser.MessageToParse;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.formats.avro.typeutils.AvroTypeInfo;
import org.apache.flink.util.InstantiationUtil;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class SerializationTests {

    @Test
    public void testThreatIntelligence() throws IOException {
        Map<String, String> map = new HashMap<String, String>() {{
            put("a", "a");
            put("b", "b");
        }};
        ThreatIntelligence ti = ThreatIntelligence.builder().fields(map)
                .observable("ob").observableType("ip").stixReference("stix").ts(0).build();
        ThreatIntelligence test = test(ti);

        assertThat(test.getFields(), equalTo(map));
    }

    @Test
    public void testEnrichmentEntry() throws IOException {
        Map<String, String> map = new HashMap<String, String>() {{
            put("a", "a");
            put("b", "b");
        }};
        EnrichmentEntry entry = EnrichmentEntry.builder()
                .type("ip")
                .key("test")
                .entries(map)
                .ts(0)
                .build();
        EnrichmentEntry test = test(entry);
        assertThat(test.getEntries(), equalTo(map));
    }

    {
        Map<String, String> map = new HashMap<String, String>() {{
            put("a", "a");
            put("b", "b");
        }};
        ThreatIntelligence ti = ThreatIntelligence.builder().fields(map)
                .observable("ob").observableType("ip").stixReference("stix").ts(0).build();
    }


    @Test
    public void testMessageWithDataQuality() throws IOException {
        Map<String, String> map = Collections.singletonMap("test", "value");
        Message m = Message.builder()
                .source("test")
                .message("")
                .ts(0)
                .originalSource(SignedSourceKey.builder().topic("test").offset(0).partition(0).signature(new byte[128]).build())
                .extensions(map)
                .dataQualityMessages(Arrays.asList(
                        DataQualityMessage.builder()
                                .field("test")
                                .level("INFO")
                                .feature("feature")
                                .message("test message")
                                .build()
                )).build();
        Message test = test(m);
        assertThat(test.getExtensions(), equalTo(map));
        assertThat(test.getDataQualityMessages().get(0).getMessage(), equalTo("test message"));
    }

    @Test
    public void testMessageToParse() throws IOException {
        MessageToParse messageToParse = MessageToParse.builder().
                offset(3).partition(1).
                originalBytes("this is a test".getBytes(UTF_8)).
                topic("test_topic").
                build();
        MessageToParse output = test(messageToParse);
        assertThat(output, equalTo(messageToParse));
    }
    
    @Test
    public void testEnrichmentCommand() throws IOException {
        Map<String, String> entryMap = Collections.singletonMap("test", "value");
        EnrichmentCommand command = EnrichmentCommand.builder()
                .type(CommandType.ADD)
                .headers(Collections.singletonMap("test", "header"))
                .payload(EnrichmentEntry.builder()
                        .type("test")
                        .ts(0)
                        .entries(entryMap)
                        .key("test")
                        .build())
                .build();
        EnrichmentCommand output = test(command);
        assertThat(output.getPayload().getEntries(), equalTo(entryMap));
    }

    @Test
    public void testEnrichmentCommandResponse() throws IOException {
        Map<String, String> entryMap = Collections.singletonMap("test", "value");
        Map<String, String> headerMap = Collections.singletonMap("test", "header");
        EnrichmentCommandResponse cr = EnrichmentCommandResponse.builder()
                .success(true)
                .message("")
                .content(Arrays.asList(EnrichmentEntry.builder()
                        .type("test")
                        .ts(0)
                        .entries(entryMap)
                        .key("test")
                        .build()))
                .headers(headerMap)
                .build();
        EnrichmentCommandResponse output = test(cr);
        assertThat(output.getHeaders(), hasEntry("test", "header"));
        assertThat(output.getContent().get(0).getEntries(), hasEntry("test", "value"));
    }

    private List<ThreatIntelligence> createTi(Map<String, String> tiFields) {
        return Arrays.asList(ThreatIntelligence.builder()
                .fields(tiFields)
                .observable("ob")
                .observableType("ip")
                .stixReference("stix")
                .ts(0)
                .build());
    }

    public static <T extends SpecificRecordBase> T test(T obj) throws IOException {
        Class cls = obj.getClass();
        AvroTypeInfo<T> ti = new AvroTypeInfo<T>(cls);
        TypeSerializer<T> serializer = ti.createSerializer(new ExecutionConfig());

        byte[] bytes = InstantiationUtil.serializeToByteArray(serializer, obj);
        T out = InstantiationUtil.deserializeFromByteArray(serializer, bytes);

        assertThat(out, notNullValue());
        return out;
    }
}
