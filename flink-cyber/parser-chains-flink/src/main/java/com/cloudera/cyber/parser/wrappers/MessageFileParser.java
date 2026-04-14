package com.cloudera.cyber.parser.wrappers;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.DataQualityMessageLevel;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.SignedSourceKey;
import com.cloudera.cyber.parser.MessageFileHeader;
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
    public static final String ZIP_FILE_CONTAINS_NO_ENTRIES_ERROR = "Zip file contains no entries.";
    public static final String HEADER_VALIDATION_ERROR = "Header validation failed: required header '%s' not found in file headers.";
    public static final String FILE_TOO_FEW_LINES_ERROR = "File has fewer lines than required header line count of %d.";
    public static final String FILE_MISSING_REQUIRED_HEADERS_ERROR = "File is missing required headers: %s.";
    private final List<String> allowedPaths;
    private final SingleMessageParser singleMessageParser;
    private final MessageFileHeader messageFileHeader;

    private MessageFileParser(List<String> allowedPaths, SingleMessageParser singleMessageParser, MessageFileHeader messageFileHeader) {
        this.allowedPaths = allowedPaths;
        this.singleMessageParser = singleMessageParser;
        this.messageFileHeader = messageFileHeader;
    }
    
    /**
     * Returns true if using line count method for header detection.
     */
    private boolean usesHeaderLineCount() {
        return messageFileHeader != null && messageFileHeader.usesHeaderLineCount();
    }
    
    /**
     * Returns true if using prefix method for header detection.
     */
    private boolean usesHeaderPrefixes() {
        return messageFileHeader != null && messageFileHeader.usesHeaderPrefixes();
    }
    
    /**
     * Returns true if header processing is configured.
     */
    private boolean hasHeader() {
        return messageFileHeader != null;
    }
    
    /**
     * Returns the header line count.
     */
    private Integer getHeaderLineCount() {
        return messageFileHeader != null ? messageFileHeader.getHeaderLineCount() : null;
    }
    
    /**
     * Returns the header prefixes.
     */
    private List<String> getHeaderPrefixes() {
        return messageFileHeader != null ? messageFileHeader.getHeaderPrefixes() : Collections.emptyList();
    }
    
    /**
     * Returns the required headers.
     */
    private List<String> getRequiredHeaders() {
        return messageFileHeader != null ? messageFileHeader.getRequiredHeaders() : Collections.emptyList();
    }

    /**
     * Verify and normalize allowed paths and create a new MessageFileParser.
     *
     * @param allowedPaths        List of paths that the parser can read when it receives a file to parse.
     * @param singleMessageParser The single message parser used to parse each line in the file.
     * @return A newly created MessageFileParser
     */
    public static MessageFileParser create(List<String> allowedPaths, SingleMessageParser singleMessageParser) {
        return create(allowedPaths, singleMessageParser, (MessageFileHeader) null);
    }

    /**
     * Verify and normalize allowed paths and create a new MessageFileParser with header support.
     *
     * @param allowedPaths        List of paths that the parser can read when it receives a file to parse.
     * @param singleMessageParser The single message parser used to parse each line in the file.
     * @param messageFileHeader Header configuration (null if no header).
     * @return A newly created MessageFileParser
     */
    public static MessageFileParser create(List<String> allowedPaths, SingleMessageParser singleMessageParser, MessageFileHeader messageFileHeader) {
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
            return new MessageFileParser(normalizedAllowedPaths, singleMessageParser, messageFileHeader);
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
                                
                                int lineNumber = 0;
                                
                                // Read header lines one by one
                                if (hasHeader()) {
                                    int skipLines = processHeaders(br);
                                    lineNumber = skipLines;
                                }
                                
                                // Now process remaining lines
                                String currentLine;
                                while ((currentLine = br.readLine()) != null) {
                                    MessageToParse messageToParse = MessageToParse.builder()
                                            .originalBytes(currentLine.getBytes(StandardCharsets.UTF_8))
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
                            if (hasHeader()) {
                                messageFileStatusExtension.put("headerSkipped", "true");
                            }
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
            } catch (IllegalArgumentException e) {
                // Re-throw IllegalArgumentException as it contains our validation errors
                String errorMessage = e.getMessage();
                sendErrorMessage(parserChainSource.getSource(), message, output, errorMessage, fileToParse);
            } catch (Exception e) {
                String errorMessage = String.format("%s with message %s", e.getClass().getName(), e.getMessage());
                sendErrorMessage(parserChainSource.getSource(), message, output, errorMessage, fileToParse);
            }
        }
    }
    
    /**
     * Process header lines from the reader.
     * Tracks found headers while reading, validates required headers on the fly.
     * 
     * @param br the BufferedReader to read from
     * @return number of lines skipped
     * @throws IOException if reading fails or required headers are missing
     */
    private int processHeaders(BufferedReader br) throws IOException {
        String line;
        int skipCount = 0;
        Set<String> foundHeaders = new HashSet<>();
        
        if (usesHeaderLineCount()) {
            // Read fixed number of header lines
            int headerLineCount = getHeaderLineCount();
            while ((line = br.readLine()) != null && skipCount < headerLineCount) {
                parseAndAddHeaderNames(line, foundHeaders);
                skipCount++;
            }
            // After reading all header lines, check if not enough
            if (skipCount < headerLineCount) {
                throw new IllegalArgumentException(String.format(FILE_TOO_FEW_LINES_ERROR, headerLineCount));
            }
        } else if (usesHeaderPrefixes()) {
            // Read lines with matching prefix
            List<String> prefixes = getHeaderPrefixes();
            while ((line = br.readLine()) != null) {
                if (matchesHeaderPrefix(line, prefixes)) {
                    parseAndAddHeaderNames(line, foundHeaders);
                    skipCount++;
                } else {
                    // First non-header line - stop reading headers
                    break;
                }
            }
        }
        
        // Validate required headers after reading all headers
        if (!getRequiredHeaders().isEmpty()) {
            validateRequiredHeaders(foundHeaders);
        }
        
        return skipCount;
    }
    
    /**
     * Check if a line matches any of the configured header prefixes.
     * Matches exactly (no parsing).
     */
    private boolean matchesHeaderPrefix(String line, List<String> prefixes) {
        for (String prefix : prefixes) {
            if (line.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Determines the number of header lines to skip based on configuration.
     * Uses either line count OR prefixes, but not both (mutually exclusive).
     * 
     * @param allLines All lines from the file.
     * @param headerLines List to populate with detected header lines.
     * @return Number of lines to skip.
     */
    private int determineHeaderLineCount(List<String> allLines, List<String> headerLines) {
        if (allLines.isEmpty()) {
            return 0;
        }
        
        // Use line count method if configured
        if (usesHeaderLineCount()) {
            int linesToSkip = Math.min(getHeaderLineCount(), allLines.size());
            for (int i = 0; i < linesToSkip; i++) {
                headerLines.add(allLines.get(i));
            }
            return linesToSkip;
        }
        
        // Use prefix method if configured
        if (usesHeaderPrefixes()) {
            int prefixSkipCount = 0;
            for (String fileLine : allLines) {
                if (isHeaderLine(fileLine)) {
                    headerLines.add(fileLine);
                    prefixSkipCount++;
                } else {
                    break;
                }
            }
            return prefixSkipCount;
        }
        
        return 0;
    }
    
    /**
     * Checks if a line is a header line based on configured prefixes.
     * 
     * @param line The line to check.
     * @return true if the line is a header line, false otherwise.
     */
    private boolean isHeaderLine(String line) {
        if (line == null || !usesHeaderPrefixes()) {
            return false;
        }
        for (String prefix : getHeaderPrefixes()) {
            if (line.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Validates that required headers exist in the found headers.
     * 
     * @param foundHeaders The set of header names found in the file.
     * @throws IllegalArgumentException if required headers are missing.
     */
    private void validateRequiredHeaders(Set<String> foundHeaders) throws IllegalArgumentException {
        List<String> missing = new ArrayList<>();
        for (String requiredHeader : getRequiredHeaders()) {
            if (!foundHeaders.contains(requiredHeader)) {
                missing.add(requiredHeader);
            }
        }
        
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(String.format(FILE_MISSING_REQUIRED_HEADERS_ERROR, String.join(", ", missing)));
        }
    }
    
    /**
     * Parses a header line and adds the header names to the set.
     * Assumes headers are in "name=value" or "name: value" format.
     * 
     * @param headerLine The header line to parse.
     * @param foundHeaders Set to add header names to.
     */
    private void parseAndAddHeaderNames(String headerLine, Set<String> foundHeaders) {
        if (headerLine == null) {
            return;
        }
        
        String[] separators = {"=", ":", "\t", " "};
        for (String sep : separators) {
            int sepIndex = headerLine.indexOf(sep);
            if (sepIndex > 0) {
                String headerName = headerLine.substring(0, sepIndex).trim();
                if (!headerName.isEmpty()) {
                    foundHeaders.add(headerName);
                }
                break;
            }
        }
        
        if (!foundHeaders.contains(headerLine.trim())) {
            foundHeaders.add(headerLine.trim());
        }
        
        if (foundHeaders.isEmpty() && !headerLine.trim().isEmpty()) {
            foundHeaders.add(headerLine.trim());
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
                    throw new IOException(ZIP_FILE_CONTAINS_NO_ENTRIES_ERROR);
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