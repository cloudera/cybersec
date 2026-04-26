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
import com.cloudera.parserchains.core.catalog.Parameter;
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
import java.util.*;

import static com.cloudera.parserchains.core.Constants.DEFAULT_INPUT_FIELD;
import static java.lang.String.format;

@MessageParser(
        name = "Simple Avro parser",
        description = "Parses Avro data by creating a field for each Avro element.")
@Slf4j
public class AvroParser implements Parser {

    public static final String DEFAULT_AVRO_SCHEMA = "netflow.schema";
    // default differs from json parser - preserve backward compatibility with existing parsers
    private static final String DEFAULT_NORMALIZER = "COLLAPSE_NESTED";

    private FieldName inputField;
    private Schema schema;
    private boolean schemaHasNestedStructure;
    private Normalizer normalizer;


    public AvroParser() {
        inputField = FieldName.of(DEFAULT_INPUT_FIELD);
        schema = null;
        normalizer = AvroParser.Normalizers.valueOf(DEFAULT_NORMALIZER);
        schemaHasNestedStructure = true;
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
    public AvroParser schemaPath(@Parameter(key = "schemaPath", label = "Schema path", isPath = true) String pathToSchema) throws IOException {
        FileSystem fileSystem = new Path(pathToSchema).getFileSystem();
        loadSchema(pathToSchema, fileSystem);
        return this;
    }

    private void loadSchema(String pathToSchema, FileSystem fileSystem) throws IOException {
        try (FSDataInputStream fsDataInputStream = fileSystem.open(new Path(pathToSchema))) {
            this.schema = new Schema.Parser().parse(fsDataInputStream);
            this.schemaHasNestedStructure = this.schema.getFields().stream().map(Schema.Field::schema).
                    anyMatch(AvroParser::isComplex);
            log.info("Successfully loaded schema {} schemaHasNestedStructure {}", pathToSchema, schemaHasNestedStructure);
        } catch (IOException ioe) {
            log.error("Exception while loading schema from file {}", pathToSchema, ioe);
            throw ioe;
        }
    }

    private static boolean isComplex(Schema schema) {
        Schema.Type type = schema.getType();

        // Handle Unions (e.g., ["null", "record"])
        if (type == Schema.Type.UNION) {
            return schema.getTypes().stream().anyMatch(AvroParser::isComplex);
        }

        // Return true if it matches one of your complex criteria
        return type == Schema.Type.RECORD ||
                type == Schema.Type.ARRAY ||
                type == Schema.Type.MAP;
    }

    @Configurable(
            key="norm",
            label="Normalizer",
            description="Defines how fields are normalized. Accepted values include: " +
                    "'COLLAPSE_NESTED' Collapse nested structures into single values.  " +
                    "'DROP_NESTED' Drop and ignore any nested structured values.  " +
                    "'UNFOLD_NESTED' Unfold the nested structures into dot-separated field names.  " +
                    "Default value: '" + DEFAULT_NORMALIZER + "'",
            defaultValue=DEFAULT_NORMALIZER
    )
    public AvroParser normalizer(String normalizer) {
        if(StringUtils.isNotBlank(normalizer)) {
            this.normalizer = AvroParser.Normalizers.valueOf(normalizer);
        }
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
            byte[] bytes = toParse.toBytes();
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            GenericDatumReader<GenericRecord> genericDatumReader = new GenericDatumReader<>(schema);
            BinaryDecoder binaryDecoder = DecoderFactory.get().binaryDecoder(byteArrayInputStream, null);
            GenericRecord genericRecord = genericDatumReader.read(null, binaryDecoder);
            if (!schemaHasNestedStructure) {
                // use collapse because the record doesn't need the more complex logic because there is nothing to skip or unfold.
               Normalizers.COLLAPSE_NESTED.normalize(genericRecord, output);
            } else {
                // use the normalizer selected in the UI
                normalizer.normalize(genericRecord, output);
            }
        } catch (IOException | AvroRuntimeException exception) {
            output.withError(exception).build();
        }
        return output.build();
    }

    /**
     * Normalizes the avro record into message fields.
     */
    private interface Normalizer {

        void normalize(GenericRecord record, Message.Builder output) throws IOException;
    }

    /**
     * Defines available {@link AvroParser.Normalizer} types.
     */
    public enum Normalizers implements AvroParser.Normalizer {
        COLLAPSE_NESTED(new CollapseNestedStructure()),
        DROP_NESTED(new DropNestedStructure()),
        UNFOLD_NESTED(new UnfoldNestedStructure());

        private final AvroParser.Normalizer normalizer;

        Normalizers(AvroParser.Normalizer normalizer) {
            this.normalizer = normalizer;
        }

        @Override
        public void normalize(GenericRecord record, Message.Builder output) throws IOException {
             normalizer.normalize(record, output);
        }
    }

    /**
     * Output only the top level fields in the record.  Collapse nested structure into a single value.
     */
    private static class CollapseNestedStructure implements AvroParser.Normalizer {

        @Override
        public void normalize(GenericRecord record, Message.Builder output) {
            // include fields only for top level record elements
            // nested records are serialized in the value
            record.getSchema().getFields().forEach(
                    field -> output.addField(field.name(), String.valueOf(record.get(field.name()))));
        }
    }

    /**
     * Drop nested structures from the output message.
     */
    private static class DropNestedStructure implements AvroParser.Normalizer {

        @Override
        public void normalize(GenericRecord record, Message.Builder output) {
            // skip complex structures
            record.getSchema().getFields().stream().filter(f -> !isComplex(f.schema())).
                    forEach(field -> output.addField(field.name(), String.valueOf(record.get(field.name()))));
        }
    }

    /**
     * Unfold the nested structures into individual message fields.
     */
    private static class UnfoldNestedStructure implements AvroParser.Normalizer {

        private void unfold(Object value, Deque<String> path, Message.Builder output) {
            if (value instanceof GenericRecord record) {
                record.getSchema().getFields().forEach(field -> {
                    path.addLast(field.name());
                    unfold(record.get(field.name()), path, output);
                    path.removeLast();
                });
            } else if (value instanceof Collection<?> list) {
                int index = 0;
                for (Object item : list) {
                    // Use bracket notation for array indices
                    path.addLast("[" + index + "]");
                    unfold(item, path, output);
                    path.removeLast();
                    index++;
                }
            } else if (value instanceof Map) {
                ((Map<?, ?>) value).forEach((k, v) -> {
                    path.addLast(k.toString());
                    unfold(v, path, output);
                    path.removeLast();
                });
            } else {
                // no structure - construct the field path and value
                output.addField(getFullyQualifiedFieldName(path), String.valueOf(value));
            }
        }

        private String getFullyQualifiedFieldName(Deque<String> path) {
            // Remove extra dot in array file - "field.[0]" becomes "field[0]"
            return String.join(".", path).replace(".[", "[");
        }

        @Override
        public void normalize(GenericRecord record, Message.Builder output) {
            unfold(record, new ArrayDeque<>(), output);
        }
    }

}

