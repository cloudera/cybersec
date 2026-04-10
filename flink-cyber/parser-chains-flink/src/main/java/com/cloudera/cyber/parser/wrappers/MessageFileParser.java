package com.cloudera.cyber.parser.wrappers;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.DataQualityMessageLevel;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.SignedSourceKey;
import com.cloudera.cyber.parser.MessageToParse;
import com.cloudera.cyber.parser.ParserChainSource;
import com.cloudera.parserchains.core.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.core.fs.FSDataInputStream;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;
import java.time.Instant;
import java.util.*;
import java.util.zip.ZipEntry;

@Slf4j
public class MessageFileParser implements ParserInterface {
    public static final String MESSAGE_FILE_PARSER_FEATURE = "message_file_parser";
    public static final String FILE_PATH_DID_NOT_MATCH_ANY_SPECIFIED_PATTERNS = "FilePath did not match any specified patterns.";
    public static final String FILE_NOT_IN_ALLOWED_PATHS = "File not in allowed paths.";
    public static final String FILE_CONTAINS_SYMBOLIC_LINKS = "File path contains symbolic link.";
    public static final String UNMATCHED_FILE_SOURCE = "unmatched_file_source";
    public static final String MESSAGE_SOURCE_FILE_STATUS = "message_file_status";
    public static final String INVALID_PATHS_MESSAGE = "The following allowed paths are invalid: %s";
    public static final String NO_ALLOWED_PATHS_SPECIFIED_FOR_MESSAGE_FILE_PARSER = "Null or empty allowed paths specified for message file parser.";
    private final List<String> allowedPaths;
    private final SingleMessageParser singleMessageParser;

    private MessageFileParser(List<String> allowedPaths, SingleMessageParser singleMessageParser) {
        this.allowedPaths = allowedPaths;
        this.singleMessageParser = singleMessageParser;
    }

    /**
     * Verify and normalize allowed paths and create a new MessageFileParser.
     *
     * @param allowedPaths        List of paths that the parser can read when it receives a file to parse.
     * @param singleMessageParser The single message parser used to parse each line in the file.
     * @return A newly created MessageFileParser
     */
    public static MessageFileParser create(List<String> allowedPaths, SingleMessageParser singleMessageParser) {
        if (allowedPaths == null || allowedPaths.isEmpty()) {
            throw new IllegalArgumentException(NO_ALLOWED_PATHS_SPECIFIED_FOR_MESSAGE_FILE_PARSER);
        }

        List<String> normalizedAllowedPaths = new ArrayList<>();
        List<String> pathsWithErrors = new ArrayList<>();

        for (String allowedPath : allowedPaths) {
            Path nextAllowedPath = new Path(allowedPath);
            try {
                if (!nextAllowedPath.isAbsolute()) {
                    logErrorPath(allowedPath, pathsWithErrors, "is not absolute");
                } else if (!nextAllowedPath.getFileSystem().getFileStatus(nextAllowedPath).isDir()) {
                    logErrorPath(allowedPath, pathsWithErrors, "is not a directory.");
                } else {
                    normalizedAllowedPaths.add(nextAllowedPath.toUri().toString());
                }
            } catch (IOException ioe) {
                logErrorPath(allowedPath, pathsWithErrors, ioe.getMessage());
            }
        }

        if (!pathsWithErrors.isEmpty()) {
            throw new IllegalArgumentException(String.format(INVALID_PATHS_MESSAGE, String.join(", ", pathsWithErrors)));
        } else {
            return new MessageFileParser(normalizedAllowedPaths, singleMessageParser);
        }
    }

    private static void logErrorPath(String pathWithError, List<String> pathsWithErrors, String errorMessage) {
        log.error("Allowed message parser path {} {}.", pathWithError, errorMessage);
        pathsWithErrors.add(pathWithError);
    }

    @Override
    public void parse(ParserChainSource parserChainSource, MessageToParse message, AbstractParserOutput output) {
        String fileToParse = new String(message.getOriginalBytes(), StandardCharsets.UTF_8);
        if (parserChainSource == null) {
            sendErrorMessage(UNMATCHED_FILE_SOURCE, message, output, FILE_PATH_DID_NOT_MATCH_ANY_SPECIFIED_PATTERNS, fileToParse);
        } else {
            try {
                Path fileToParsePath = checkForSymbolicLinks(fileToParse);
                if (fileToParsePath == null) {
                    sendErrorMessage(parserChainSource.getSource(), message, output, FILE_CONTAINS_SYMBOLIC_LINKS, fileToParse);
                } else {
                    String fileToParsePathString = fileToParsePath.toUri().toString();
                    if (allowedPaths.stream().anyMatch(fileToParsePathString::startsWith)) {
                        FileSystem fileSystem = fileToParsePath.getFileSystem();
                        try (FSDataInputStream is = fileSystem.open(fileToParsePath)) {
                            try (InputStream decompressedStream = createDecompressionStream(is, fileToParsePath.toUri().toString());
                                 BufferedReader br = new BufferedReader(new InputStreamReader(decompressedStream, StandardCharsets.UTF_8))) {
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
                    } else {
                        sendErrorMessage(parserChainSource.getSource(), message, output, FILE_NOT_IN_ALLOWED_PATHS, fileToParse);
                    }
                }
            } catch (Exception e) {
                String errorMessage = String.format("%s with message %s", e.getClass().getName(), e.getMessage());
                sendErrorMessage(parserChainSource.getSource(), message, output, errorMessage, fileToParse);
            }
        }
    }

        private Path checkForSymbolicLinks(String fileToParse) throws IOException {
            Path fileToParsePath = new Path(fileToParse);
            FileSystem fileSystem = fileToParsePath.getFileSystem();
            if (!fileSystem.isDistributedFS()) {

                java.nio.file.Path nioPath = Paths.get(fileToParsePath.toUri().toString());
                java.nio.file.Path realPath = nioPath.toRealPath();
                if (!nioPath.toString().equalsIgnoreCase(realPath.toString())) {
                    fileToParsePath = null;
                }
            }
            return fileToParsePath;
        }

        /**
         * Creates an input stream that handles decompression for gzip and zip compressed files,
         * or returns the original stream if the file is not compressed.
         *
         * @param inputStream The input stream to decompress if needed
         * @param filePath The file path (used to determine compression type from extension)
         * @return A stream that provides decompressed data
         * @throws IOException If decompression fails
         */
        private InputStream createDecompressionStream(InputStream inputStream, String filePath) throws IOException {
            String pathLower = filePath.toLowerCase();
            if (pathLower.endsWith(".gz") || pathLower.endsWith(".gzip")) {
                return new GZIPInputStream(inputStream);
            } else if (pathLower.endsWith(".zip")) {
                ZipInputStream zipIn = new ZipInputStream(inputStream, StandardCharsets.UTF_8);
                ZipEntry entry = zipIn.getNextEntry();
                if (entry == null) {
                    throw new IOException("Zip file contains no entries: " + filePath);
                }
                return zipIn;
            }
            return inputStream;
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