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

package com.cloudera.cyber;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hortonworks.registries.schemaregistry.SchemaCompatibility;
import com.hortonworks.registries.schemaregistry.SchemaIdVersion;
import com.hortonworks.registries.schemaregistry.SchemaMetadata;
import com.hortonworks.registries.schemaregistry.client.ISchemaRegistryClient;
import com.hortonworks.registries.schemaregistry.errors.IncompatibleSchemaException;
import com.hortonworks.registries.schemaregistry.errors.InvalidSchemaException;
import com.hortonworks.registries.schemaregistry.errors.SchemaNotFoundException;
import com.hortonworks.registries.schemaregistry.serdes.avro.AvroSnapshotSerializer;
import com.hortonworks.registries.schemaregistry.serdes.avro.AvroUtils;
import java.time.Instant;
import java.util.Collections;
import org.apache.avro.Schema;
import org.junit.Before;
import org.junit.Test;

public class TestMessageSerializer {

    private AvroSnapshotSerializer avroSnapshotSerializer;

    private Message testMessage() {
        return Message.builder()
              .ts(Instant.now().toEpochMilli())
              .originalSource(SignedSourceKey.builder()
                    .topic("test")
                    .partition(0)
                    .offset(0)
                    .signature(new byte[128])
                    .build())
              .extensions(Collections.singletonMap("test", "value"))
              .message("")
              .source("test")
              .build();
    }

    private ThreatIntelligence testTi() {
        return ThreatIntelligence.builder()
              .ts(Instant.now().toEpochMilli())
              .fields(Collections.singletonMap("test", "value"))
              .observableType("testType")
              .observable("testObservable")
              .build();
    }

    @Before
    public void init() throws SchemaNotFoundException, InvalidSchemaException, IncompatibleSchemaException {
        ISchemaRegistryClient testClient = mock(ISchemaRegistryClient.class);
        when(testClient.uploadSchemaVersion(any(), any(), any(), any()))
              .thenReturn(new SchemaIdVersion(1L, 1));

        when(testClient.addSchemaVersion(any(SchemaMetadata.class), any()))
              .thenReturn(new SchemaIdVersion(1L, 1, 1L));

        avroSnapshotSerializer = new AvroSnapshotSerializer(testClient);
        avroSnapshotSerializer.init(Collections.emptyMap());

    }

    @Test
    public void testSimpleAvroBased() {
        Message test = testMessage();

        SchemaMetadata schemaMetadata = new SchemaMetadata.Builder("test")
              .schemaGroup("kafka")
              .description("test")
              .type("avro")
              .compatibility(SchemaCompatibility.FORWARD).build();
        byte[] serialize = avroSnapshotSerializer.serialize(test, schemaMetadata);

        assertThat("Bytes are made", serialize.length, greaterThan(100));
    }

    @Test
    public void testMessageSchema() {
        Schema schema = AvroUtils.computeSchema(testMessage());

        System.out.println(schema.toString());
        assertThat("Schema Computed for Message", schema, notNullValue());
        assertThat("Schema has fields", schema.getField("extensions"), notNullValue());
        assertThat("Schema has timestamp field", schema.getField("ts").name(), equalTo("ts"));
    }

    @Test
    public void testTISchema() {
        Schema schema = AvroUtils.computeSchema(testTi());

        assertThat("Schema Computed for ThreatIntelligence", schema, notNullValue());
        assertThat("Schema has fields", schema.getField("fields"), notNullValue());
    }

    @Test
    public void testTIMessageSerializer() {
        ThreatIntelligence test = testTi();

        SchemaMetadata schemaMetadata = new SchemaMetadata.Builder("test")
              .schemaGroup("kafka")
              .description("test")
              .type("avro")
              .compatibility(SchemaCompatibility.FORWARD).build();


        byte[] serialize = avroSnapshotSerializer.serialize(test, schemaMetadata);
        assertThat("Bytes are made", serialize.length, equalTo(85));
    }
}
