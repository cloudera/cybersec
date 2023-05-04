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
import com.cloudera.parserchains.core.Message;
import com.cloudera.parserchains.core.Parser;
import com.cloudera.parserchains.core.catalog.Configurable;
import com.cloudera.parserchains.core.catalog.MessageParser;
import com.cloudera.parserchains.core.utils.JSONUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.cloudera.parserchains.core.Constants.DEFAULT_INPUT_FIELD;
import static java.lang.String.format;

@MessageParser(
        name="Simple JSON",
        description="Parses JSON data by creating a field for each JSON element.")
public class JSONParser implements Parser {
    private static final String DEFAULT_NORMALIZER = "UNFOLD_NESTED";
    private FieldName inputField;
    private ObjectReader reader;
    private List<Normalizer> normalizers;

    public JSONParser() {
        inputField = FieldName.of(DEFAULT_INPUT_FIELD);
        reader = new ObjectMapper().readerFor(Map.class);
        normalizers = new ArrayList<>();
    }

    @Configurable(
            key="input",
            label="Input Field",
            description="The input field to parse.",
            defaultValue=DEFAULT_INPUT_FIELD)
    public JSONParser inputField(String fieldName) {
        if(StringUtils.isNotBlank(fieldName)) {
            this.inputField = FieldName.of(fieldName);
        }
        return this;
    }

    @Configurable(
            key="norm",
            label="Normalizer",
            description="Defines how fields are normalized. Accepted values include: " +
                    "'ALLOW_NESTED' Embed nested JSON string as the field value.  " +
                    "'DISALLOW_NESTED' Stop parsing and throw an error if nested JSON exists.  " +
                    "'DROP_NESTED' Drop and ignore any nested JSON values.  " +
                    "'UNFOLD_NESTED' Unfold the nested JSON by creating a nested, dot-separated field name.  ",
            defaultValue=DEFAULT_NORMALIZER,
            multipleValues = true)
    public JSONParser normalizer(String normalizer) {
        if(StringUtils.isNotBlank(normalizer)) {
            addNormalizer(Normalizers.valueOf(normalizer));
        }
        return this;
    }

    void addNormalizer (Normalizers normalizer) {
        normalizers.add(Objects.requireNonNull(normalizer, "A normalizer is required."));
    }

    @Override
    public Message parse(Message input) {
        Message.Builder output = Message.builder().withFields(input);
        if(!input.getField(inputField).isPresent()) {
            output.withError(format("Message missing expected input field '%s'", inputField.toString()));
        } else {
            input.getField(inputField).ifPresent(val -> doParse(val.toString(), output));
        }
        return output.build();
    }

    public void doParse(String toParse, Message.Builder output) {
        try {
            Map<String, Object> values = reader.readValue(toParse);
            values = normalize(values, output);
            values.forEach((key, value) -> output.addField(key, value.toString()));

        } catch (IOException e) {
            output.withError(e);
        }
    }

    private Map<String, Object> normalize(Map<String, Object> valueToNormalize, Message.Builder output) {
        // use the default normalizer, if none other specified
        if(normalizers.size() == 0) {
            normalizer(DEFAULT_NORMALIZER);
        }
        try {
            for (Normalizer normalizer : normalizers) {
                valueToNormalize = normalizer.normalize(valueToNormalize);
            }
        } catch(IOException e) {
            output.withError("Failed to normalize.", e);
        }
        return valueToNormalize;
    }

    /**
     * Defines all of the available {@link Normalizer} types.
     */
    private enum Normalizers implements Normalizer {
        ALLOW_NESTED(new AllowNestedObjects()),
        DISALLOW_NESTED(new DisallowNestedObjects()),
        DROP_NESTED(new DropNestedObjects()),
        UNFOLD_NESTED(new UnfoldNestedObjects());

        private Normalizer normalizer;

        Normalizers(Normalizer normalizer) {
            this.normalizer = normalizer;
        }

        @Override
        public Map<String, Object> normalize(Map<String, Object> input) throws IOException {
            return normalizer.normalize(input);
        }
    }

    /**
     * Normalizes the parsed JSON input.
     */
    private interface Normalizer {
        Map<String, Object> normalize(Map<String, Object> input) throws IOException;
    }

    /**
     * Allow nested objects.
     */
    private static class AllowNestedObjects implements Normalizer {
        @Override
        public Map<String, Object> normalize(Map<String, Object> input) throws JsonProcessingException {
            Map<String, Object> output = new HashMap<>();
            for(Map.Entry<String, Object> entry: input.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                serializeNested(key, value, output);
            }
            return output;
        }

        private void serializeNested(String rootKey, Object valueToUnfold, Map<String, Object> output) throws JsonProcessingException {
            if (valueToUnfold instanceof Map) {
                // handle nested JSON objects
                String serialized = JSONUtils.INSTANCE.toJSON(valueToUnfold, false);
                output.put(rootKey, serialized);

            } else if (valueToUnfold instanceof List) {
                // handle JSON arrays
                String serialized = JSONUtils.INSTANCE.toJSON(valueToUnfold, false);
                output.put(rootKey, serialized);

            } else {
                output.put(rootKey, valueToUnfold);
            }
        }
    }

    /**
     * An error is thrown if any nested objects exist.
     */
    private static class DisallowNestedObjects implements Normalizer {
        @Override
        public Map<String, Object> normalize(Map<String, Object> input) throws IOException {
            // throw an exception if any nested objects exist
            Optional<Object> nestedObject = input.values()
                    .stream()
                    .filter(v -> v instanceof Map || v instanceof List)
                    .findFirst();
            if(nestedObject.isPresent()) {
                throw new IOException("Nested objects are not allowed.");
            }
            return input;
        }
    }

    /**
     * Drop any nested objects.
     */
    private static class DropNestedObjects implements Normalizer {
        @Override
        public Map<String, Object> normalize(Map<String, Object> input) {
            // drop any that is a JSON object (aka Map)
            return input.entrySet()
                    .stream()
                    .filter(e -> !(e.getValue() instanceof Map) && !(e.getValue() instanceof List))
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        }
    }

    /**
     * Unfold any nested objects.
     */
    private static class UnfoldNestedObjects implements Normalizer {
        @Override
        public Map<String, Object> normalize(Map<String, Object> input) {
            Map<String, Object> output = new HashMap<>();
            unfold(null, input, output);
            return output;
        }

        private void unfold(String rootKey, Object valueToUnfold, Map<String, Object> output) {
            if (valueToUnfold instanceof Map) {
                // handle nested JSON objects
                Map<String, Object> mapValue = (Map) valueToUnfold;
                mapValue.forEach((key, value) -> {
                    String newKey = rootKey != null ? String.join(".", rootKey, key) : key;
                    unfold(newKey, value, output);
                });

            } else if (valueToUnfold instanceof List) {
                // handle JSON arrays
                List<Object> listValue = (List) valueToUnfold;
                for(int i=0; i<listValue.size(); i++) {
                    Object value = listValue.get(i);
                    String newKey = rootKey + "[" + i + "]";
                    unfold(newKey, value, output);
                }

            } else {
                output.put(rootKey, valueToUnfold);
            }
        }
    }
}
