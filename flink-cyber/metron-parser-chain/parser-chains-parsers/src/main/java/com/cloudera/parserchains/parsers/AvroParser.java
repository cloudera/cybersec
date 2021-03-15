package com.cloudera.parserchains.parsers;

import static com.cloudera.parserchains.core.Constants.DEFAULT_INPUT_FIELD;
import static java.lang.String.format;

import com.cloudera.parserchains.core.FieldName;
import com.cloudera.parserchains.core.FieldValue;
import com.cloudera.parserchains.core.Message;
import com.cloudera.parserchains.core.Parser;
import com.cloudera.parserchains.core.catalog.Configurable;
import com.cloudera.parserchains.core.catalog.MessageParser;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.commons.lang3.StringUtils;

@MessageParser(
        name = "Simple Avro parser",
        description = "Parses Avro data by creating a field for each Avro element.")
public class AvroParser implements Parser {

    public static final String DEFAULT_AVRO_SCHEMA = "avro-9009.schema";

    private FieldName inputField;
    private Schema schema;

    public AvroParser() {
        inputField = FieldName.of(DEFAULT_INPUT_FIELD);
        schema = null;
    }

    @Configurable(
            key = "input",
            label = "Input Field",
            description = "The input field to parse.",
            defaultValue = DEFAULT_INPUT_FIELD)
    public AvroParser inputField(String fieldName) {
        if (StringUtils.isNotBlank(fieldName)) {
            this.inputField = FieldName.of(fieldName);
        }
        return this;
    }

    @Configurable(
            key = "schemaPath",
            label = "Schema Path",
            description = "Path to schema of avro file",
            defaultValue = DEFAULT_AVRO_SCHEMA)
    public AvroParser schemaPath(String pathToSchema) throws URISyntaxException, IOException {
        URI uriPath = Objects.requireNonNull(getClass().getClassLoader().getResource(pathToSchema))
                .toURI();
        this.schema = new Schema.Parser().parse(new File(uriPath));
        return this;
    }

    @Override
    public Message parse(Message input) {
        Message.Builder builder = Message.builder().withFields(input);
        Optional<FieldValue> field = input.getField(inputField);
        if (field.isPresent()) {
            return doParse(field.get(), builder);
        } else {
            return builder
                    .withError(format("Message missing expected input field '%s'", inputField.toString()))
                    .build();
        }
    }

    public Message doParse(FieldValue toParse, Message.Builder output) {
        try {
            byte[] bytes = toParse.get().getBytes(StandardCharsets.UTF_8);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            GenericDatumReader<GenericRecord> genericDatumReader = new GenericDatumReader<>(schema);
            BinaryDecoder binaryDecoder = DecoderFactory.get().binaryDecoder(byteArrayInputStream, null);
            GenericRecord genericRecord = genericDatumReader.read(null, binaryDecoder);
            genericRecord.getSchema().getFields().forEach(
                    field -> output.addField(field.name(), String.valueOf(genericRecord.get(field.name()))));
        } catch (IOException exception) {
            output.withError(exception).build();
        }
        return output.build();
    }
}

