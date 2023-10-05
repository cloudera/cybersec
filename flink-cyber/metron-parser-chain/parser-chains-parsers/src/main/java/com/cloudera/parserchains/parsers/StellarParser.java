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

import static com.cloudera.parserchains.core.Constants.DEFAULT_INPUT_FIELD;
import com.cloudera.cyber.parser.MessageToParse;
import com.cloudera.cyber.stellar.MetronCompatibilityParser;
import com.cloudera.parserchains.core.FieldName;
import com.cloudera.parserchains.core.FieldValue;
import com.cloudera.parserchains.core.Message;
import com.cloudera.parserchains.core.Parser;
import com.cloudera.parserchains.core.catalog.Configurable;
import com.cloudera.parserchains.core.catalog.MessageParser;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.core.fs.FSDataInputStream;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;
import org.apache.metron.parsers.interfaces.MessageParserResult;
import org.json.simple.JSONObject;

@MessageParser(
        name = "Metron Stellar parser",
        description = "Metron compatibility parser.")
@Slf4j
public class StellarParser implements Parser {

    private FieldName inputField;
    private MetronCompatibilityParser metronCompatibilityParser;


    public StellarParser() {
        inputField = FieldName.of(DEFAULT_INPUT_FIELD);
    }

    @Configurable(
            key = "input",
            label = "Input Field",
            description = "The input field to parse. Default value: '" + DEFAULT_INPUT_FIELD + "'",
            defaultValue = DEFAULT_INPUT_FIELD)
    public StellarParser inputField(String fieldName) {
        if (StringUtils.isNotBlank(fieldName)) {
            this.inputField = FieldName.of(fieldName);
        }
        return this;
    }

    @Configurable(
            key = "configurationPath",
            label = "Configuration File Path",
            description = "Path to parser config file",
            required = true)
    public StellarParser configurationPath(String pathToSchema) throws IOException {
        FileSystem fileSystem = new Path(pathToSchema).getFileSystem();
        loadParser(pathToSchema, fileSystem);
        return this;
    }

    private void loadParser(String pathToConfig, FileSystem fileSystem) throws IOException {
        Path configPath = new Path(pathToConfig);
        try (FSDataInputStream fsDataInputStream = fileSystem.open(configPath)) {
            String fileName = configPath.getName();
            String sensorType = fileName.substring(0, fileName.lastIndexOf('.'));
            this.metronCompatibilityParser = MetronCompatibilityParser.of(sensorType, fsDataInputStream);
        } catch (Exception e) {
            log.error(String.format("Could not create parser from '%s'", pathToConfig), e);
            throw e;
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
                    .withError(String.format("Message missing expected input field '%s'", inputField.toString()))
                    .build();
        }
    }

    private Message doParse(FieldValue toParse, Message.Builder output) {
        MessageToParse messageToParse = toParse.toMessageToParse();
        Optional<MessageParserResult<JSONObject>> optionalResult = metronCompatibilityParser.parse(messageToParse);
        if (optionalResult.isPresent()) {
            MessageParserResult<JSONObject> result = optionalResult.get();
            Optional<Throwable> messageException = result.getMasterThrowable();
            if (messageException.isPresent()) {
                output.withError(messageException.get().getMessage());
            } else {
                // this parser can only return a single message - return the first message
                List<JSONObject> parsedMessages = result.getMessages();
                if (CollectionUtils.isNotEmpty(parsedMessages)) {
                    JSONObject jsonMessage = parsedMessages.get(0);
                    Set<Map.Entry<String, Object>> entries = jsonMessage.entrySet();
                    for (Map.Entry<String, Object> entry : entries) {
                        output.addField(entry.getKey(), String.valueOf(entry.getValue()));
                    }
                } else {
                    // message was filtered
                    output.emit(false).build();
                }
            }
        } else {
            output.withError("Parser did not return a message result").build();
        }
        return output.build();
    }

}