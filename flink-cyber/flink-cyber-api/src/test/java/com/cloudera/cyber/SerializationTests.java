package com.cloudera.cyber;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.formats.avro.typeutils.AvroTypeInfo;
import org.apache.flink.util.InstantiationUtil;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
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

    private List<ThreatIntelligence> createTi(Map<String, String> tiFields) {
        return Arrays.asList(ThreatIntelligence.builder()
                .fields(tiFields)
                .observable("ob")
                .observableType("ip")
                .stixReference("stix")
                .ts(0)
                .build());
    }

    public <T extends SpecificRecordBase> T test(T obj) throws IOException {
        Class cls = obj.getClass();
        AvroTypeInfo<T> ti = new AvroTypeInfo<T>(cls);
        TypeSerializer<T> serializer = ti.createSerializer(new ExecutionConfig());

        byte[] bytes = InstantiationUtil.serializeToByteArray(serializer, obj);
        T out = InstantiationUtil.deserializeFromByteArray(serializer, bytes);

        assertThat(out, notNullValue());
        return out;
    }
}
