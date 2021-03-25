package com.cloudera.parserchains.parsers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import com.cloudera.parserchains.core.FieldName;
import com.cloudera.parserchains.core.FieldValue;
import com.cloudera.parserchains.core.Message;
import com.google.common.collect.ImmutableMap;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.avro.Schema;
import org.apache.avro.SchemaParseException;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

class AvroParserTest {

    private final AvroParser parser = new AvroParser();
    private static final String SCHEMA_PATH = "/avro/avro.schema";
    private static final String BROKEN_SCHEMA = "/avro/bad-avro.schema";
    private static final String BAD_AVRO_DATA = "/avro/bad-avro-data.avro";
    private static final String DATA_PATH = "/avro/avro-data.avro";
    private static final String INPUT_FIELD = "source";

    @Test
    public void testSchemaFileRead() throws IOException {
        String schemaPath = getFileFromResource(SCHEMA_PATH).getAbsolutePath();

        parser.schemaPath(schemaPath).inputField(INPUT_FIELD);
        Message parsedMessage = parser.parse(buildMessageFromFile(DATA_PATH));

        assertThat(parsedMessage.getFields()).contains(
                entry(FieldName.of("name"), FieldValue.of("Ben")),
                entry(FieldName.of("number"), FieldValue.of("7")),
                entry(FieldName.of("innerRecord"), FieldValue.of("{\"age\": 13}")),
                entry(FieldName.of("tes3"), FieldValue.of("{td3=1}")));
    }

    @Test
    public void testSchemaDataRead() throws IOException {
        String schemaPath = getFileFromResource(SCHEMA_PATH).getAbsolutePath();

        parser.schemaPath(schemaPath).inputField(INPUT_FIELD);
        Message parsedMessage = parser.parse(buildMessage());

        assertThat(parsedMessage.getFields()).contains(
                entry(FieldName.of("name"), FieldValue.of("Tom")),
                entry(FieldName.of("number"), FieldValue.of("22")),
                entry(FieldName.of("innerRecord"), FieldValue.of("{\"age\": 42}")),
                entry(FieldName.of("tes3"), FieldValue.of("{key11=11, key22=22}")));
    }

    @Test
    public void testIncorrectSchema() {
        String schemaPath = getFileFromResource(BROKEN_SCHEMA).getAbsolutePath();

        assertThatThrownBy(() -> parser.schemaPath(schemaPath)).isInstanceOf(SchemaParseException.class);
    }

    @Test
    public void testNotExistSchemaRead() {
        String schemaPath = "/some/file.schema";
        assertThatThrownBy(() -> parser.schemaPath(schemaPath)).isInstanceOf(IOException.class).hasMessageContaining("some/file.schema");
    }

    @Test
    public void testIfMessageIsIncorrect() throws IOException {
        String schemaPath = getFileFromResource(SCHEMA_PATH).getAbsolutePath();
        String missingField = "missing field";
        Message message = parser.schemaPath(schemaPath).inputField(missingField).parse(buildMessageFromFile(DATA_PATH));

        assertThat(message.getError()).hasValueSatisfying( ex -> {
           assertThat(ex).isInstanceOf(IllegalStateException.class).hasMessage("Message missing expected input field '"+ missingField + "'");
        });
    }

    @Test
    public void testBadAvroData() throws IOException {
        String schemaPath = getFileFromResource(SCHEMA_PATH).getAbsolutePath();
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
        return new File(AvroParserTest.class.getResource(path).getFile());
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
        return Message.builder().addField(INPUT_FIELD, new String(out.toByteArray(), StandardCharsets.UTF_8)).build();
    }

}