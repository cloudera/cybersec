package com.cloudera.parserchains.parsers;
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

import com.cloudera.parserchains.core.*;
import com.cloudera.parserchains.core.catalog.Configurable;
import com.cloudera.parserchains.core.catalog.MessageParser;
import com.cloudera.parserchains.core.catalog.Parameter;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.krakens.grok.api.Grok;
import io.krakens.grok.api.GrokCompiler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Slf4j
@MessageParser(
        name = "Grok Table",
        description = "Extracts a key using an initial grok expression and parses the message using a grok expression mapped from a table."
)
public class GrokTableParser extends AbstractTextInputParser {
    public static final String KEY_FIELD_NAME_CONFIGURATION = "Key Field Name";
    public static final String MESSAGE_FIELD_NAME_CONFIGURATION = "Message Field Name";
    public static final String INITIAL_GROK_EXPRESSION_CONFIGURATION = "Initial Grok Expression";
    public static final String GROK_PATTERN_PATH_CONFIGURATION = "Grok Pattern Path";
    public static final String INPUT_FIELD_CONFIGURATION = "Input Field";
    public static final String MISSING_REQUIRED_CONFIGURATION_FIELD_ERROR_FORMAT = "Missing required configuration field(s): %s";
    public static final String PATTERN_LOADING_ERROR_FORMAT = "Pattern files '%s' could not be loaded";
    public static final String MESSAGE_TO_PARSE_EXPECTED_INPUT_FIELD_ERROR_FORMAT = "Message to parse expected input field '%s'";
    public static final String FAILED_MESSAGE_GROK_COMPILATION_ERROR_FORMAT = "Grok compilation error in message '%s': '%s'";
    public static final String MESSAGE_KEY_FIELD_NULL_AFTER_GROK_CAPTURE_ERROR_FORMAT = "Message key field '%s' is null after grok capture";
    public static final String MESSAGE_FIELD_NULL_AFTER_GROK_CAPTURE_ERROR_FORMAT = "Message field '%s' is null after grok capture";

    enum ValidationState {
        UNVALIDATED,
        VALID,
        INVALID
    }

    private FieldName inputField;
    private final GrokCompiler grokCompiler;
    private Grok initialGrokExpression;
    private String keyFieldName;
    private String messageFieldName;
    private static class GrokCompilerResult {
        Grok compiledGrok;
        String errorMessage;

        private GrokCompilerResult(Grok compiledGrok, String errorMessage) {
            this.compiledGrok = compiledGrok;
            this.errorMessage = errorMessage;
        }

         static GrokCompilerResult create(String grokExpression, GrokCompiler grokCompiler) {
             Grok compiledGrok = null;
             String errorMessage = null;
             try {
                 compiledGrok = grokCompiler.compile(grokExpression, true);
             } catch (Exception e) {
                 errorMessage = e.getMessage();
             }
             return new GrokCompilerResult(compiledGrok, errorMessage);
         }
    }
    private final Cache<String, GrokCompilerResult> grokTable;
    private ValidationState isValid = ValidationState.UNVALIDATED;
    private String validationError = null;
    private final Map<String, Boolean> patternsLoaded = new HashMap<>();

    public GrokTableParser() {
        inputField = FieldName.of(Constants.DEFAULT_INPUT_FIELD);
        grokCompiler = GrokCompiler.newInstance();
        grokCompiler.registerDefaultPatterns();
        initialGrokExpression = null;
        keyFieldName = null;
        messageFieldName = null;
        grokTable = Caffeine.newBuilder()
                .maximumSize(100)
                .build();
    }

    @Override
    public Message parse(Message input) {
        Message.Builder output = Message.builder().withFields(input);
        if (!isConfigured()) {
            output.withError(validationError);
        } else if (!input.getField(inputField).isPresent()) {
            output.withError(format(MESSAGE_TO_PARSE_EXPECTED_INPUT_FIELD_ERROR_FORMAT, inputField.toString()));
        } else {
            input.getField(inputField).ifPresent(val -> doParse(val.toString(), output));
        }
        return output.build();
    }

    private boolean isConfigured() {
        if (isValid == ValidationState.UNVALIDATED) {
            List<String> missingFields = new ArrayList<>();
            checkIfNonBlank(keyFieldName, KEY_FIELD_NAME_CONFIGURATION, missingFields);
            checkIfNonBlank(messageFieldName, MESSAGE_FIELD_NAME_CONFIGURATION, missingFields);
            if (initialGrokExpression == null) {
                missingFields.add(INITIAL_GROK_EXPRESSION_CONFIGURATION);
            }
            if (patternsLoaded.isEmpty()) {
                missingFields.add((GROK_PATTERN_PATH_CONFIGURATION));
            }
            if (!missingFields.isEmpty()) {
                validationError = String.format(MISSING_REQUIRED_CONFIGURATION_FIELD_ERROR_FORMAT, StringUtils.join(missingFields, ", "));
                isValid = ValidationState.INVALID;
            } else if (patternsLoaded.entrySet().stream().anyMatch(e -> !e.getValue())) {
                String failedPatternFiles = patternsLoaded.entrySet().stream()
                        .filter(entry -> !entry.getValue()) // Filter where value is false
                        .map(Map.Entry::getKey) // Get the keys
                        .collect(Collectors.joining(", "));
                validationError = String.format(PATTERN_LOADING_ERROR_FORMAT,failedPatternFiles);
                isValid = ValidationState.INVALID;
            } else {
                isValid = ValidationState.VALID;
            }
        }
        return (isValid == ValidationState.VALID);
    }

    private void checkIfNonBlank(String configuredValue, String fieldName, List<String> missingFields ) {
        if (StringUtils.isBlank(configuredValue)) {
            missingFields.add(fieldName);
        }
    }

    private void doParse(String textToParse, Message.Builder output) {
        Map<String, Object> grokResult = initialGrokExpression.match(textToParse)
                .capture()
                .entrySet()
                .stream()
                .filter(e -> e.getKey() != null && e.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        String key = getGrokString(grokResult, keyFieldName);
        String message = getGrokString(grokResult, messageFieldName);
        // add the base message fields to the output
        grokResult.forEach((field_name, field_value) -> output.addField(field_name, field_value.toString()));
        if (key == null) {
            output.withError(format(MESSAGE_KEY_FIELD_NULL_AFTER_GROK_CAPTURE_ERROR_FORMAT, keyFieldName));
        } else if (message == null) {
            output.withError(format(MESSAGE_FIELD_NULL_AFTER_GROK_CAPTURE_ERROR_FORMAT, messageFieldName));
        } else {
            doMessageGrok(key, message, output);
        }
    }

    private String getGrokString(Map<String, Object> grokResult, String fieldName) {
        Object valueObject = grokResult.get(fieldName);
        String valueString = null;

        if (valueObject != null) {
            valueString = valueObject.toString();
            if (StringUtils.isBlank(valueString)) {
                valueString = null;
            }
        }

        return valueString;
    }

    private void doMessageGrok(String messageKey, String textToParse, Message.Builder output) {
        String sanitizedMessageKey = messageKey.replaceAll("[^A-z0-9_]+", "_");
        String messagePattern = grokCompiler.getPatternDefinitions().get(sanitizedMessageKey);
        // if there is a pattern, defined for this message key, capture the pattern
        // if not, just return the fields extracted by the initial grok expression
        if (messagePattern != null) {
            GrokCompilerResult grokResult = grokTable.get(messagePattern, pattern -> GrokCompilerResult.create(messagePattern, grokCompiler));
            if (grokResult != null) {
                if (grokResult.compiledGrok != null) {
                    grokResult.compiledGrok.match(textToParse)
                            .capture()
                            .entrySet()
                            .stream()
                            .filter(e -> e.getKey() != null && e.getValue() != null)
                            .forEach(e -> output.addField(e.getKey(), e.getValue().toString()));
                } else {
                    output.withError(format(FAILED_MESSAGE_GROK_COMPILATION_ERROR_FORMAT, sanitizedMessageKey, grokResult.errorMessage));
                }
            }
        }
    }

    @Configurable(key = "keyFieldName",
            label = KEY_FIELD_NAME_CONFIGURATION,
            description = "The name of the key field extracted from the initialGrokExpression.",
            required = true)
      public GrokTableParser keyFieldName(String keyFieldName) {

        if (StringUtils.isNoneBlank(keyFieldName)) {
            this.keyFieldName = keyFieldName;
        }

        return this;
    }

    @Configurable(key = "messageFieldName",
            label = MESSAGE_FIELD_NAME_CONFIGURATION,
            description = "The name of the message field extracted from the initialGrokExpression.",
            required = true)
    public GrokTableParser messageFieldName(String messageFieldName) {

        if (StringUtils.isNoneBlank(messageFieldName)) {
            this.messageFieldName = messageFieldName;
        }

        return this;
    }

    @Configurable(key = "initialGrokExpression",
            label = INITIAL_GROK_EXPRESSION_CONFIGURATION,
            description = "The initial grok expression used to extract the key and message.",
            required = true)
    public GrokTableParser initialGrokExpression(String initialGrokExpression) {
        if (StringUtils.isNotBlank(initialGrokExpression)) {
            this.initialGrokExpression = grokCompiler.compile(initialGrokExpression, true);
        }
        return this;
    }

    @Configurable(
            key = "grokPatternPath",
            label = GROK_PATTERN_PATH_CONFIGURATION,
            description = "Path to file containing, grok patterns used in key grok expression and mapping message keys to expressions.  Define pattern name then whitespace followed by pattern expression.  Define a pattern for each message type that you want to extract.  Replace all non alphanumeric characters with underscores.",
            orderPriority = 1)
    public GrokTableParser grokPatternPath(@Parameter(key = "grokPatternPath", isPath = true,
            label = GROK_PATTERN_PATH_CONFIGURATION,
            description = "Path to file containing, grok patterns used in key grok expression and mapping message keys to expressions.  Define pattern name then whitespace followed by pattern expression.  Define a pattern for each message type that you want to extract.  Replace all non alphanumeric characters with underscores.",
            required = true) String grokPatternPath) {
        if (StringUtils.isNotBlank(grokPatternPath)) {
            try {
                patternsLoaded.put(grokPatternPath, false);
                FileSystem fileSystem = new Path(grokPatternPath).getFileSystem();
                Path path = new Path(grokPatternPath);
                log.info("Loading grok patterns from {}", path);
                try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileSystem.open(path)))) {
                    this.grokCompiler.register(bufferedReader);
                    patternsLoaded.put(grokPatternPath, true);
                }
                catch (IOException e) {
                    log.error("Failed to load grok patterns from {}", path, e);
                    throw new IllegalArgumentException("Invalid path while load grok patterns from " + grokPatternPath, e);
                }
            } catch (Exception e) {
                log.error("Invalid path while load grok patterns from {}", grokPatternPath, e);
                throw new IllegalArgumentException("Invalid path while load grok patterns from " + grokPatternPath, e);
            }
        }

        return this;
    }

    public GrokTableParser inputField(FieldName inputField) {
        if (inputField != null && StringUtils.isNotBlank(inputField.get())) {
            this.inputField = inputField;
        }
        return this;
    }

    @Configurable(key = "inputField",
            label = INPUT_FIELD_CONFIGURATION,
            description = "The name of the input field to parse. Default value: '" + Constants.DEFAULT_INPUT_FIELD + "'",
            defaultValue = Constants.DEFAULT_INPUT_FIELD)
    public GrokTableParser inputField(String inputField) {
        if (StringUtils.isNotBlank(inputField)) {
            this.inputField = FieldName.of(inputField);
        }
        return this;
    }

    public FieldName getInputField() {
        return inputField;
    }

}
