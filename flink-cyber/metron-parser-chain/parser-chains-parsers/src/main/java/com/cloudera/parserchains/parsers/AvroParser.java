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

import com.cloudera.parserchains.core.FieldName;
import com.cloudera.parserchains.core.FieldValue;
import com.cloudera.parserchains.core.Message;
import com.cloudera.parserchains.core.Parser;
import com.cloudera.parserchains.core.catalog.Configurable;
import com.cloudera.parserchains.core.catalog.MessageParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.core.fs.FSDataInputStream;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

import static com.cloudera.parserchains.core.Constants.DEFAULT_INPUT_FIELD;
import static java.lang.String.format;

@MessageParser(
        name = "Simple Avro parser",
        description = "Parses Avro data by creating a field for each Avro element.")
@Slf4j
public class AvroParser implements Parser {

    public static final String DEFAULT_AVRO_SCHEMA = "netflow.schema";

    private FieldName inputField;
    private Schema schema;

    public AvroParser() {
        inputField = FieldName.of(DEFAULT_INPUT_FIELD);
        schema = null;
    }

    @Configurable(
            key = "input",
            label = "Input Field",
            description = "The input field to parse. Default value: '" + DEFAULT_INPUT_FIELD + "'",
            defaultValue = DEFAULT_INPUT_FIELD,
            isOutputName = true)
    public AvroParser inputField(String fieldName) {
        if (StringUtils.isNotBlank(fieldName)) {
            this.inputField = FieldName.of(fieldName);
        }
        return this;
    }

    @Configurable(
            key = "schemaPath",
            label = "Schema Path",
            description = "Path to schema of avro file. Default value: '" + DEFAULT_AVRO_SCHEMA + "'",
            defaultValue = DEFAULT_AVRO_SCHEMA,
            required = true)
    public AvroParser schemaPath(String pathToSchema) throws IOException {
        FileSystem fileSystem = new Path(pathToSchema).getFileSystem();
        loadSchema(pathToSchema, fileSystem);
        return this;
    }

    private void loadSchema(String pathToSchema, FileSystem fileSystem) throws IOException {
        try (FSDataInputStream fsDataInputStream = fileSystem.open(new Path(pathToSchema))) {
            this.schema = new Schema.Parser().parse(fsDataInputStream);
            log.info("Successfully loaded schema {}", pathToSchema);
        } catch (IOException ioe) {
            log.error("Exception while loading schema from file " + pathToSchema, ioe);
            throw ioe;
        }
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
            byte[] bytes = toParse.toBytes();
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            GenericDatumReader<GenericRecord> genericDatumReader = new GenericDatumReader<>(schema);
            BinaryDecoder binaryDecoder = DecoderFactory.get().binaryDecoder(byteArrayInputStream, null);
            GenericRecord genericRecord = genericDatumReader.read(null, binaryDecoder);
            genericRecord.getSchema().getFields().forEach(
                    field -> output.addField(field.name(), String.valueOf(genericRecord.get(field.name()))));
        } catch (IOException | AvroRuntimeException exception) {
            output.withError(exception).build();
        }
        return output.build();
    }
}

