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

package com.cloudera.parserchains.parsers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.cloudera.cyber.parser.MessageToParse;
import com.cloudera.parserchains.core.FieldName;
import com.cloudera.parserchains.core.Message;
import com.cloudera.parserchains.core.MessageToParseFieldValue;
import com.cloudera.parserchains.core.StringFieldValue;
import com.google.common.collect.ImmutableMap;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.apache.avro.Schema;
import org.apache.avro.SchemaParseException;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.commons.io.FileUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.Test;

class AvroParserTest {

    private static final String SCHEMA_PATH = "/avro/avro.schema";
    private static final String BROKEN_SCHEMA = "/avro/bad-avro.schema";
    private static final String BAD_AVRO_DATA = "/avro/bad-avro-data.avro";
    /**
     * Schema without any nested records, arrays or maps.
     */
    private static final String SIMPLE_SCHEMA = "/avro/simple.schema";
    private static final String NESTED_SCHEMA = "/avro/log_event.schema";
    private static final String DATA_PATH = "/avro/avro-data.avro";
    private static final String INPUT_FIELD = "source";

    @Test
    public void testSchemaFileRead() throws IOException {
        String schemaPath = getFileFromResource(SCHEMA_PATH).getAbsolutePath();

        AvroParser parser = new AvroParser();
        parser.schemaPath(schemaPath).inputField(INPUT_FIELD);
        Message parsedMessage = parser.parse(buildMessageFromFile(DATA_PATH));

        assertThat(parsedMessage.getFields()).contains(
                entry(FieldName.of("name"), StringFieldValue.of("Ben")),
                entry(FieldName.of("number"), StringFieldValue.of("7")),
                entry(FieldName.of("innerRecord"), StringFieldValue.of("{\"age\": 13}")),
                entry(FieldName.of("tes3"), StringFieldValue.of("{td3=1}")));
        assertFalse(parsedMessage.getError().isPresent());
    }

    @Test
    public void testSchemaDataReadWithDefaultNormalizer() throws IOException {
        String schemaPath = getFileFromResource(SCHEMA_PATH).getAbsolutePath();

        AvroParser defaultNormalizerParser = new AvroParser();
        defaultNormalizerParser.schemaPath(schemaPath).inputField(INPUT_FIELD);
        testDefaultNormalizer(defaultNormalizerParser);

        AvroParser emptyNormalizerParser = new AvroParser();
        emptyNormalizerParser.schemaPath(schemaPath).normalizer("").inputField(INPUT_FIELD);
        testDefaultNormalizer(emptyNormalizerParser);
    }

    private void testDefaultNormalizer(AvroParser parser) throws IOException {

        Message parsedMessage = parser.parse(buildMessage());

        assertThat(parsedMessage.getFields()).contains(
                entry(FieldName.of("name"), StringFieldValue.of("Tom")),
                entry(FieldName.of("number"), StringFieldValue.of("22")),
                entry(FieldName.of("innerRecord"), StringFieldValue.of("{\"age\": 42}")),
                entry(FieldName.of("tes3"), StringFieldValue.of("{key11=11, key22=22}")));
        assertFalse(parsedMessage.getError().isPresent());
    }

    @Test
    public void testSchemaDataReadWithUnknownNormalizer() {
        String schemaPath = getFileFromResource(SCHEMA_PATH).getAbsolutePath();
        AvroParser parser = new AvroParser();

        assertThatCode(() -> parser.schemaPath(schemaPath).inputField(INPUT_FIELD).normalizer("UNKNOWN")).
                isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid normalizer: 'UNKNOWN'. Valid values are: COLLAPSE_NESTED, DROP_NESTED, UNFOLD_NESTED");
    }

    @Test
    public void testIgnoreUnfoldingForSimpleRecords() throws IOException {
        String schemaPath = getFileFromResource(SIMPLE_SCHEMA).getAbsolutePath();
        AvroParser parser = new AvroParser();

        parser.schemaPath(schemaPath).inputField(INPUT_FIELD).normalizer(AvroParser.Normalizers.UNFOLD_NESTED.name());
        Map<String, Object> recordFields = ImmutableMap.of("userId", 1,
                "username", "Jane Doe",
                "email", "jdoe@acme.com",
                "isActive", true,
                "age", 42);
        Message input = serializeMapToAvroMessage(recordFields);
        Message parsedMessage = parser.parse(input);


        for(Map.Entry<String,Object> entry: recordFields.entrySet()) {
            assertThat(parsedMessage.getFields()).contains(entry(FieldName.of(entry.getKey()), StringFieldValue.of(String.valueOf(entry.getValue()))));
        }
        assertFalse(parsedMessage.getError().isPresent());
    }

    @Test
    public void testUnfoldingNestedStructures() throws IOException {
        testNestedStructures(AvroParser.Normalizers.UNFOLD_NESTED.name());
    }

    @Test
    public void testDroppingNestedStructures() throws IOException {
        testNestedStructures(AvroParser.Normalizers.DROP_NESTED.name());
    }

    @Test
    public void testCollapsingNestedStructures() throws IOException {
        testNestedStructures(AvroParser.Normalizers.COLLAPSE_NESTED.name());
    }

    @Test
    public void testCollapsingNestedStructuresWithNulls() throws IOException {
        AvroParser parser = new AvroParser();
        String schemaPath = getFileFromResource(NESTED_SCHEMA).getAbsolutePath();


        parser.inputField(INPUT_FIELD).normalizer(AvroParser.Normalizers.UNFOLD_NESTED.name()).schemaPath(schemaPath);

        Message parsedMessage = parser.parse(createStructuredMessageWithNulls());

        assertFalse(parsedMessage.getError().isPresent());
        assertThat(parsedMessage.getFields()).contains(
                entry(FieldName.of("source.osVersion"), StringFieldValue.of("null")),
                entry(FieldName.of("source.hostName"), StringFieldValue.of("web-gateway-03")),
                entry(FieldName.of("parsedVariables.referrer"), StringFieldValue.of("null")),
                entry(FieldName.of("parsedVariables.http_method"), StringFieldValue.of("GET")),
                entry(FieldName.of("parsedVariables.request_size"), StringFieldValue.of("1024")),
                entry(FieldName.of("templateTags[0]"), StringFieldValue.of("WEB")),
                entry(FieldName.of("templateTags[1]"), StringFieldValue.of("INFO")),
                entry(FieldName.of("rawLog"), StringFieldValue.of("2026-04-25T15:05:00 GET /index.html 200")),
                entry(FieldName.of("eventStatus"), StringFieldValue.of("null"))
        );
    }

    private Message createStructuredMessageWithNulls() throws IOException {
        Schema schema = new Schema.Parser().parse(getFileFromResource(NESTED_SCHEMA));
        // Nested Record: LogSource
        GenericRecord source = new GenericData.Record(schema.getField("source").schema());
        source.put("hostName", "web-gateway-03");
        source.put("osVersion", null); // Explicitly setting optional field to null

        // Map: parsedVariables
        Map<String, Object> variables = new HashMap<>();
        variables.put("http_method", "GET");
        variables.put("request_size", 1024L);
        variables.put("referrer", null); // Using the 'null' type in the map value union

        // Array: templateTags
        List<String> tags = Arrays.asList("WEB", "INFO");

        // Main Record: LogEvent
        GenericRecord record = new GenericData.Record(schema);
        record.put("rawLog", "2026-04-25T15:05:00 GET /index.html 200");
        record.put("source", source);
        record.put("templateTags", tags);
        record.put("parsedVariables", variables);
        record.put("eventStatus", null);

        return serializeAvroToMessage(record, schema);
    }

    private void testNestedStructures(String normalizerName) throws IOException {

        AvroParser parser = new AvroParser();
        String schemaPath = getFileFromResource(NESTED_SCHEMA).getAbsolutePath();
        Schema parentSchema = new Schema.Parser().parse(getFileFromResource(NESTED_SCHEMA));
        Schema nestedSchema = parentSchema.getField("source").schema();

        parser.inputField(INPUT_FIELD).normalizer(normalizerName).schemaPath(schemaPath);

        GenericRecord record = createFullNestedRecord(nestedSchema, parentSchema);
        Message input = serializeAvroToMessage(record, parentSchema);
        Message parsedMessage = parser.parse(input);

        assertFalse(parsedMessage.getError().isPresent());
        assertThat(parsedMessage.getFields()).contains(
                entry(FieldName.of("rawLog"), StringFieldValue.of("2026-04-25T15:00:00 [ERROR] failed to connect to database")),
                entry(FieldName.of("eventStatus"), StringFieldValue.of("FAILED"))
        );
        if (normalizerName.equals(AvroParser.Normalizers.UNFOLD_NESTED.name())) {
            assertThat(parsedMessage.getFields()).contains(
                    entry(FieldName.of("source.osVersion"), StringFieldValue.of("RHEL 8.6")),
                    entry(FieldName.of("source.hostName"), StringFieldValue.of("log-server-01")),
                    entry(FieldName.of("parsedVariables.event_time_ms"), StringFieldValue.of("1777183165")),
                    entry(FieldName.of("parsedVariables.user_agent"), StringFieldValue.of("Mozilla/5.0")),
                    entry(FieldName.of("parsedVariables.process_id"), StringFieldValue.of("4567")),
                    entry(FieldName.of("templateTags[0]"), StringFieldValue.of("NETWORK")),
                    entry(FieldName.of("templateTags[1]"), StringFieldValue.of("HIGH_SEVERITY")),
                    entry(FieldName.of("templateTags[2]"), StringFieldValue.of("INGESTION_V2"))
                    );
        } else if (normalizerName.equals(AvroParser.Normalizers.COLLAPSE_NESTED.name())) {
            assertThat(parsedMessage.getFields()).contains(
                    entry(FieldName.of("source"), StringFieldValue.of("{\"hostName\": \"log-server-01\", \"osVersion\": \"RHEL 8.6\"}")),
                    entry(FieldName.of("parsedVariables"), StringFieldValue.of("{process_id=4567, event_time_ms=1777183165, user_agent=Mozilla/5.0}")),
                    entry(FieldName.of("templateTags"), StringFieldValue.of("[NETWORK, HIGH_SEVERITY, INGESTION_V2]"))
            );
        }
    }

    private @NonNull GenericRecord createFullNestedRecord(Schema nestedSchema, Schema parentSchema) {
        // Nested Record: LogSource
        GenericRecord source = new GenericData.Record(nestedSchema);
        source.put("hostName", "log-server-01");
        source.put("osVersion", "RHEL 8.6"); // Populating the optional string field

        // Map: parsedVariables
        Map<String, Object> variables = new HashMap<>();
        variables.put("event_time_ms", 1777183165L); // Using the 'long' type in the map value union
        variables.put("process_id", 4567L);
        variables.put("user_agent", "Mozilla/5.0"); // Using the 'string' type in the map value union

        // Array: templateTags
        List<String> tags = Arrays.asList("NETWORK", "HIGH_SEVERITY", "INGESTION_V2");

        // Main Record: LogEvent
        GenericRecord record = new GenericData.Record(parentSchema);
        record.put("rawLog", "2026-04-25T15:00:00 [ERROR] failed to connect to database");
        record.put("source", source);
        record.put("templateTags", tags);
        record.put("parsedVariables", variables);
        record.put("eventStatus", "FAILED"); // Using the 'string' type in the status union
        return record;
    }

    /**
     * Serializes a GenericRecord into an Avro binary byte array.
     *
     * @param record The Avro GenericRecord to serialize.
     * @param schema The Schema used to interpret the record.
     * @return A byte array containing the Avro binary data.
     * @throws IOException If serialization fails.
     */
    private static Message serializeAvroToMessage(GenericRecord record, Schema schema) throws IOException {

        GenericDatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(schema);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);

            datumWriter.write(record, encoder);
            encoder.flush();
            return getOriginalMessage(outputStream);
        }
    }

    private static Message serializeMapToAvroMessage(Map<String, Object> fields) throws IOException {
        Schema schema = new Schema.Parser().parse(getFileFromResource(AvroParserTest.SIMPLE_SCHEMA));

        GenericRecord record = new GenericData.Record(schema);
        fields.forEach(record::put);

        return serializeAvroToMessage(record, schema);
    }

    @Test
    public void testIncorrectSchema() {
        String schemaPath = getFileFromResource(BROKEN_SCHEMA).getAbsolutePath();
        AvroParser parser = new AvroParser();
        assertThatThrownBy(() -> parser.schemaPath(schemaPath)).isInstanceOf(SchemaParseException.class);
    }

    @Test
    public void testNotExistSchemaRead() {
        String schemaPath = "/some/file.schema";
        AvroParser parser = new AvroParser();
        assertThatThrownBy(() -> parser.schemaPath(schemaPath)).isInstanceOf(IOException.class).hasMessageContaining("some/file.schema");
    }

    @Test
    public void testIfMessageIsIncorrect() throws IOException {
        String schemaPath = getFileFromResource(SCHEMA_PATH).getAbsolutePath();
        String missingField = "missing field";
        AvroParser parser = new AvroParser();
        Message message = parser.schemaPath(schemaPath).inputField(missingField).parse(buildMessageFromFile(DATA_PATH));

        assertThat(message.getError()).hasValueSatisfying( ex -> assertThat(ex).isInstanceOf(IllegalStateException.class).
                hasMessage("Message missing expected input field '"+ missingField + "'"));
    }

    @Test
    public void testBadAvroData() throws IOException {
        String schemaPath = getFileFromResource(SCHEMA_PATH).getAbsolutePath();
        AvroParser parser = new AvroParser();
        parser.schemaPath(schemaPath).inputField(INPUT_FIELD);
        Message message = parser.parse(buildMessageFromFile(BAD_AVRO_DATA));
        assertThat(message.getError()).isNotEmpty().get().isInstanceOf(IOException.class);
    }

    private static Message buildMessageFromFile(String path) throws IOException {
        File file = getFileFromResource(path).getAbsoluteFile();
        String dataFile = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        return Message.builder().addField(INPUT_FIELD, dataFile).build();
    }

    private static File getFileFromResource(String path) {
        return new File(Objects.requireNonNull(AvroParserTest.class.getResource(path)).getFile());
    }


    private static Message buildMessage() throws IOException {
        Schema schema = new Schema.Parser().parse(getFileFromResource(SCHEMA_PATH));
        Schema innerSchema = schema.getField("innerRecord").schema();
        GenericRecordBuilder innerRecordBuilder = new GenericRecordBuilder(innerSchema);
        innerRecordBuilder.set("age", 42);
        GenericData.Record innerRecord = innerRecordBuilder.build();
        GenericRecordBuilder recordBuilder = new GenericRecordBuilder(schema);
        recordBuilder.set("name", "Tom");
        recordBuilder.set("number", 22);
        recordBuilder.set("innerRecord", innerRecord);
        recordBuilder.set("tes3", ImmutableMap.of("key11", 11, "key22", 22));
        GenericData.Record record = recordBuilder.build();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinaryEncoder binaryEncoder = EncoderFactory.get().directBinaryEncoder(out, null);
        GenericDatumWriter<GenericRecord> writer = new GenericDatumWriter<>(schema);
        writer.write(record, binaryEncoder);
        return getOriginalMessage(out);
    }

    private static Message getOriginalMessage(ByteArrayOutputStream out) {
        MessageToParse originalMessage = MessageToParse.builder().originalBytes(out.toByteArray()).line(-1).partition(1).offset(1).build();
        return Message.builder().addField(FieldName.of(INPUT_FIELD), MessageToParseFieldValue.of(originalMessage)).build();
    }


}