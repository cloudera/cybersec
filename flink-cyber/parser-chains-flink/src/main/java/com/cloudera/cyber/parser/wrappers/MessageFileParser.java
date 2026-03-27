package com.cloudera.cyber.parser.wrappers;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.DataQualityMessageLevel;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.SignedSourceKey;
import com.cloudera.cyber.parser.MessageToParse;
import com.cloudera.cyber.parser.ParserChainSource;
import com.cloudera.parserchains.core.Constants;
import org.apache.flink.core.fs.FSDataInputStream;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class MessageFileParser implements ParserInterface {
    public static final String MESSAGE_FILE_PARSER_FEATURE = "message_file_parser";
    public static final String FILE_PATH_DID_NOT_MATCH_ANY_SPECIFIED_PATTERNS = "FilePath did not match any specified patterns.";
    public static final String UNMATCHED_FILE_SOURCE = "unmatched_file_source";
    public static final String MESSAGE_SOURCE_FILE_STATUS = "message_file_status";
    private final SingleMessageParser singleMessageParser;

    public MessageFileParser(SingleMessageParser singleMessageParser) {
        this.singleMessageParser = singleMessageParser;
    }

    @Override
    public void parse(ParserChainSource parserChainSource, MessageToParse message, AbstractParserOutput output) {
        String fileToParse = new String(message.getOriginalBytes(), StandardCharsets.UTF_8);
        if (parserChainSource != null) {
            try {
                FileSystem fileSystem = new Path(fileToParse).getFileSystem();
                Path fileToParsePath = new Path(fileToParse);

                try (FSDataInputStream is = fileSystem.open(fileToParsePath)) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        String line;
                        int lineNumber = 1;
                        while ((line = br.readLine()) != null) {

                            MessageToParse messageToParse = MessageToParse.builder()
                                    .originalBytes(line.getBytes(StandardCharsets.UTF_8))
                                    .topic(message.getTopic())
                                    .offset(message.getOffset())
                                    .partition(message.getPartition())
                                    .key(null)
                                    .line(lineNumber++)
                                    .build();
                            singleMessageParser.parse(parserChainSource, messageToParse, output);
                        }
                    }
                    Map<String, String> messageFileStatusExtension = new HashMap<>();
                    messageFileStatusExtension.put("filePath", fileToParse);
                    messageFileStatusExtension.put("modificationTime", String.valueOf(fileSystem.getFileStatus(fileToParsePath).getModificationTime()));
                    messageFileStatusExtension.put("successMessageCount", String.valueOf(output.getSuccessfulMessages()));
                    messageFileStatusExtension.put("errorMessageCount", String.valueOf(output.getErrorMessages()));
                    output.outputMessage(Message.builder().
                            ts(Instant.now().toEpochMilli()).
                            source(MESSAGE_SOURCE_FILE_STATUS).
                            originalSource(SignedSourceKey.builder().
                                    topic(message.getTopic()).partition(message.getPartition()).
                                    offset(message.getOffset()).signature(SingleMessageParser.EMPTY_SIGNATURE).build()).
                            extensions(messageFileStatusExtension).
                            build());
                }

            } catch (IOException fileSystemIOException) {
                String errorMessage = String.format("IOException with message %s", fileSystemIOException.getMessage());
                sendErrorMessage(parserChainSource.getSource(), message, output, errorMessage, fileToParse);
            }
        } else {
            sendErrorMessage(UNMATCHED_FILE_SOURCE, message, output, FILE_PATH_DID_NOT_MATCH_ANY_SPECIFIED_PATTERNS, fileToParse);
        }
    }

    private void sendErrorMessage(String source, MessageToParse message, AbstractParserOutput output, String errorText, String fileToParse) {
        Map<String, String> extensions = new HashMap<>();
        extensions.put(Constants.DEFAULT_INPUT_FIELD, fileToParse);

        Message errorMessage = Message.builder()
                .ts(Instant.now().toEpochMilli())
                .source(source)
                .extensions(extensions)
                .originalSource(
                        SignedSourceKey.builder()
                                .topic(message.getTopic())
                                .partition(message.getPartition())
                                .offset(message.getOffset())
                                .signature(new byte[1])
                                .build())
                .dataQualityMessages(Collections.singletonList(
                        DataQualityMessage.builder().
                                field(Constants.DEFAULT_INPUT_FIELD).
                                feature(MESSAGE_FILE_PARSER_FEATURE).
                                level(DataQualityMessageLevel.ERROR.name()).
                                message(errorText).
                                build()))
                .build();
        output.outputMessage(errorMessage);
    }
}