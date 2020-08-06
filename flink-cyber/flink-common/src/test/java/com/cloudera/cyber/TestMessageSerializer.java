package com.cloudera.cyber;

import com.hortonworks.registries.schemaregistry.SchemaCompatibility;
import com.hortonworks.registries.schemaregistry.SchemaIdVersion;
import com.hortonworks.registries.schemaregistry.SchemaMetadata;
import com.hortonworks.registries.schemaregistry.client.ISchemaRegistryClient;
import com.hortonworks.registries.schemaregistry.errors.IncompatibleSchemaException;
import com.hortonworks.registries.schemaregistry.errors.InvalidSchemaException;
import com.hortonworks.registries.schemaregistry.errors.SchemaNotFoundException;
import com.hortonworks.registries.schemaregistry.serdes.avro.AvroSnapshotSerializer;
import com.hortonworks.registries.schemaregistry.serdes.avro.AvroUtils;
import org.apache.avro.Schema;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestMessageSerializer {

    private AvroSnapshotSerializer avroSnapshotSerializer;
    private ISchemaRegistryClient testClient;

    private Message testMessage() {
        return Message.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setTs(DateTime.now())
                .setOriginalSource("test")
                .setExtensions(Collections.singletonMap("test", "value"))
                .build();
    }

    private ThreatIntelligence testTi() {
        return ThreatIntelligence.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setTs(DateTime.now())
                .setFields(Collections.singletonMap("test", "value"))
                .setObservableType("testType")
                .setObservable("testObservable")
                .setStixReference("stix")
                .build();
    }

    @Before
    public void init() throws SchemaNotFoundException, InvalidSchemaException, IncompatibleSchemaException {
        testClient = mock(ISchemaRegistryClient.class);
        when(testClient.uploadSchemaVersion(any(),any(),any(),any()))
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

        assertThat("Bytes are made", serialize.length, equalTo(71));
    }

    @Test
    public void testMessageSchema() {
        Schema schema = AvroUtils.computeSchema(testMessage());

        System.out.println(schema.toString());
        assertThat("Schema Computed for Message", schema, notNullValue());
        assertThat("Schema has fields", schema.getField("extensions"), notNullValue());
        assertThat("Schema has timestamp field", schema.getField("ts").name(),equalTo("ts"));
    }

    @Test
    public void testTISchema() {
        Schema schema = AvroUtils.computeSchema(testTi());

        assertThat("Schema Computed for ThreatIntelligence", schema, notNullValue());
        assertThat("Schema has fields", schema.getField("fields"), notNullValue());
    }

    @Test
    public void testTIMessageSerializer() throws IOException {
        ThreatIntelligence test = testTi();

        SchemaMetadata schemaMetadata = new SchemaMetadata.Builder("test")
                .schemaGroup("kafka")
                .description("test")
                .type("avro")
                .compatibility(SchemaCompatibility.FORWARD).build();


        byte[] serialize = avroSnapshotSerializer.serialize(test, schemaMetadata);
        assertThat("Bytes are made", serialize.length, equalTo(92));

    }
}
