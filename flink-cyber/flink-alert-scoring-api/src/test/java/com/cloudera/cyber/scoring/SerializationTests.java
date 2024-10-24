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

package com.cloudera.cyber.scoring;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import com.cloudera.cyber.rules.RuleType;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.apache.avro.Schema;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.formats.avro.typeutils.AvroTypeInfo;
import org.apache.flink.util.InstantiationUtil;
import org.junit.Ignore;
import org.junit.Test;

public class SerializationTests {

    @Test
    public void testScore() throws IOException {
        Scores test = Scores.builder().ruleId(UUID.randomUUID().toString()).reason("test").score(1.0).build();
        Scores out = test(test);
        assertThat(out, equalTo(test));
    }

    @Test
    @Ignore
    public void testScoringRule() throws IOException {
        ScoringRule test = ScoringRule.builder()
              .id(UUID.randomUUID().toString())
              .enabled(true)
              .name("test")
              .order(1)
              .ruleScript("test()")
              .tsStart(Instant.now())
              .tsEnd(Instant.now().plus(5, ChronoUnit.MINUTES))
              .type(RuleType.JS)
              .version(0)
              .build();

        ScoringRule out = test(test);
        assertThat(out, equalTo(test));
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

    @Test
    public void schemaTest() {
        Schema schema = ReflectData.get().getSchema(ScoringRule.class);
        assertThat(schema, notNullValue());
        assertThat(schema, equalTo(ScoringRule.SCHEMA$));
    }
}
