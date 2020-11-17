package com.cloudera.cyber;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.formats.avro.typeutils.AvroTypeInfo;
import org.apache.flink.util.InstantiationUtil;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class QuickTests {

    @Test
    public void testMessage() throws IOException {
        Map<String, String> map = Collections.singletonMap("test", "value");
        Message m = Message.builder()
                .source("test")
                .message("")
                .ts(0)
                .originalSource(SignedSourceKey.builder().topic("test").offset(0).partition(0).signature(new byte[128]).build())
                .extensions(map)
                .build();

        Message test = test(m);
        assertThat(test.getExtensions(), equalTo(map));
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
