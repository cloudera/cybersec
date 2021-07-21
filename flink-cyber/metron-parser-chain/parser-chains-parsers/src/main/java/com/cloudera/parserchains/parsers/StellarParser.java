package com.cloudera.parserchains.parsers;

import com.cloudera.cyber.stellar.MetronCompatibilityParser;
import com.cloudera.parserchains.core.FieldName;
import com.cloudera.parserchains.core.FieldValue;
import com.cloudera.parserchains.core.Message;
import com.cloudera.parserchains.core.Parser;
import com.cloudera.parserchains.core.catalog.Configurable;
import com.cloudera.parserchains.core.catalog.MessageParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.core.fs.FSDataInputStream;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;
import org.apache.metron.parsers.interfaces.MessageParserResult;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.cloudera.parserchains.core.Constants.DEFAULT_INPUT_FIELD;

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
            description = "The input field to parse.",
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
            description = "Path to parser config file")
    public StellarParser configurationPath(String pathToSchema) throws IOException {
        FileSystem fileSystem = new Path(pathToSchema).getFileSystem();
        loadParser(pathToSchema, fileSystem);
        return this;
    }

    private void loadParser(String pathToConfig, FileSystem fileSystem) throws IOException {
        try (FSDataInputStream fsDataInputStream = fileSystem.open(new Path(pathToConfig))) {
            this.metronCompatibilityParser = MetronCompatibilityParser.of(fsDataInputStream);
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
        byte[] bytes = toParse.get().getBytes(StandardCharsets.UTF_8);
        Optional<MessageParserResult<JSONObject>> optionalResult = metronCompatibilityParser.parse(bytes);
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
                    output.withError("Parser returned an empty message result").build();
                }
            }
        } else {
            output.withError("Parser did not return a message result").build();
        }
        return output.build();
    }

}