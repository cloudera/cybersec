package com.cloudera.cyber.parser.wrappers;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.parser.*;
import com.cloudera.parserchains.core.InvalidParserException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.cloudera.cyber.parser.wrappers.MessageFileParser.*;
import static com.cloudera.cyber.parser.wrappers.SingleMessageParser.EMPTY_SIGNATURE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MessageFileParserTest {

    private static final String DOESNT_EXIST_INVALID_ALLOWED_PATH = MessageFileParserTestUtil.getValidMessageFileAllowedPath().concat("/does_not_exist");
    private static final String RELATIVE_INVALID_ALLOWED_PATH = "./relative_path_not_allowed";

    @TempDir
    File testTempDir;

    @Test
    public void testSuccessful() throws NoSuchAlgorithmException, InvalidKeyException, InvalidParserException, IOException {
        MessageFileParser parser = createMessageFileParserToTest(Collections.singletonList(MessageFileParserTestUtil.getValidMessageFileAllowedPath()));

        MessageToParse messageToParse = MessageFileParserTestUtil.createMessageToParse("message_file/vpc_flow_samples.txt");
        ParserTestUtils.TestParserOutput parserOutput = new ParserTestUtils.TestParserOutput();
        parser.parse(new ParserChainSource("vpcflow", "netflow", null), messageToParse, parserOutput);

        MessageFileParserTestUtil.verifyMessageFileOutput(parserOutput.getOutput(), messageToParse);
    }

    @Test
    public void testCreateFailsWithRelativeAllowedPaths()  {
      testInvalidAllowedPath(RELATIVE_INVALID_ALLOWED_PATH);
    }

    @Test
    public void testFileWithSymLinkFails() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidParserException {
        Path linkPath = Paths.get(testTempDir.toString(), "message_file");
        Files.createSymbolicLink(linkPath, Paths.get(MessageFileParserTestUtil.getValidMessageFileAllowedPath()));

        String fileContainsLink = Paths.get(linkPath.toString(), "vpc_flow_samples.txt").toString();
        testErrorCase(fileContainsLink,  Collections.singletonList(testTempDir.toString()), new ParserChainSource("vpcflow", "netflow", null), FILE_CONTAINS_SYMBOLIC_LINKS);

    }

    @Test
    public void testCreateFailedWithFileInAllowedPaths() {
        testInvalidAllowedPath(MessageFileParserTestUtil.getValidMessageFileAllowedPath().concat("/vpc_flow_samples.txt"));
    }

    @Test
    public void testCreateFailedWithDirectoryDoesNotExist() {
        testInvalidAllowedPath(DOESNT_EXIST_INVALID_ALLOWED_PATH);
    }

    private void testInvalidAllowedPath(String invalidPath) {
        assertThatThrownBy(() ->createMessageFileParserToTest(Collections.singletonList(invalidPath))).isInstanceOf(IllegalArgumentException.class).hasMessage(String.format(INVALID_PATHS_MESSAGE, invalidPath));
    }

    @Test
    public void testCreateFailedWithEmptyAllowedDirectories() {
        testNoAllowedDirectories(null);
        testNoAllowedDirectories(Collections.emptyList());
    }

    private void testNoAllowedDirectories(List<String> nullOrEmptyAllowedMessages) {
        assertThatThrownBy(() ->createMessageFileParserToTest(nullOrEmptyAllowedMessages)).isInstanceOf(IllegalArgumentException.class).hasMessage(NO_ALLOWED_PATHS_SPECIFIED_FOR_MESSAGE_FILE_PARSER);
    }

    private MessageFileParser createMessageFileParserToTest(List<String> allowedPaths) throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidParserException {
        ParserChainMap parserChainMap = ParserTestUtils.readParserChainMap("message_file/VpcFlowChain.json");
        return MessageFileParser.create(allowedPaths, SingleMessageParser.create(parserChainMap, null));
    }

    @Test
    public void createMultipleValidAndInvalidPaths() {
        List<String> invalidPaths = Arrays.asList(DOESNT_EXIST_INVALID_ALLOWED_PATH,
                RELATIVE_INVALID_ALLOWED_PATH);
        List<String>   validPaths = Arrays.asList(
                MessageFileParserTestUtil.getValidMessageFileAllowedPath(),
                ParserTestUtils.resolveResourcePath("metron"));

        assertThatThrownBy(() -> createMessageFileParserToTest(
                Stream.concat(validPaths.stream(), invalidPaths.stream()).collect(Collectors.toList()))).
                isInstanceOf(IllegalArgumentException.class).
                hasMessage(String.format(INVALID_PATHS_MESSAGE, String.join(", ", invalidPaths)));
    }

    @Test
    public void filePathNotInAllowedDirectory() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidParserException {
        String fileNotInAllowedPath = ParserTestUtils.resolveResourcePath("metron/samples/oraclelogon.txt");
        testErrorCase(fileNotInAllowedPath,  new ParserChainSource("vpcflow", "netflow", null), FILE_NOT_IN_ALLOWED_PATHS);
    }

    @Test
    public void nullParserChainIndicatingNoMatchError() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidParserException {
        testErrorCase("no_match_for_pattern", null, FILE_PATH_DID_NOT_MATCH_ANY_SPECIFIED_PATTERNS);
    }

    @Test
    public void fileDoesntExistError() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidParserException {
        String fileDoesntExist = MessageFileParserTestUtil.getValidMessageFileAllowedPath().concat("/doesnt_exist");
        testErrorCase(fileDoesntExist, new ParserChainSource("vpcflow", "netflow", null),
                String.format("java.nio.file.NoSuchFileException with message %s", fileDoesntExist));
    }

    private static void testErrorCase(String filePath, ParserChainSource parserChainSource,String expectedErrorMessage) throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidParserException {
        final List<String> defaultAllowedPaths = Collections.singletonList(MessageFileParserTestUtil.getValidMessageFileAllowedPath());
        testErrorCase(filePath, defaultAllowedPaths, parserChainSource, expectedErrorMessage);
    }

    private static void testErrorCase(String filePath, List<String> allowedPaths, ParserChainSource parserChainSource,String expectedErrorMessage) throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidParserException {
        ParserChainMap parserChainMap = ParserTestUtils.readParserChainMap("message_file/VpcFlowChain.json");

        SingleMessageParser singleMessageParser = SingleMessageParser.create(parserChainMap, null);
        MessageFileParser parser = MessageFileParser.create(allowedPaths, singleMessageParser);

        MessageToParse messageToParse = MessageFileParserTestUtil.createMessageToParse(filePath);
        ParserTestUtils.TestParserOutput parserOutput = new ParserTestUtils.TestParserOutput();
        String expectedSource = (parserChainSource != null) ? parserChainSource.getSource() : UNMATCHED_FILE_SOURCE;
        parser.parse(parserChainSource, messageToParse, parserOutput);

        MessageFileParserTestUtil.verifyErrorMessage(parserOutput, messageToParse, filePath, expectedSource, expectedErrorMessage, EMPTY_SIGNATURE);
    }

    @Test
    public void testParseGzippedFile() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidParserException {
        // Create a temporary gzipped file
        String sampleContent = "10.0.0.1 10.0.0.2 443 443 6 120 120 162 OK Ingress\n" +
                        "10.0.0.3 10.0.0.4 80 80 6 60 60 162 OK Egress";
        Path tempDirPath = testTempDir.toPath();
        Path gzipFile = Files.createTempFile(tempDirPath, "test_data", ".gz");
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(new FileOutputStream(gzipFile.toFile()))) {
            gzipOut.write(sampleContent.getBytes(StandardCharsets.UTF_8));
        }

        testCompressedFileParsing(gzipFile);
    }

    private void testCompressedFileParsing(Path compressedFile) throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidParserException {
        ParserTestUtils.TestParserOutput parserOutput = getFileParserOutput(compressedFile);

        // Should successfully parse 2 messages from the gzip file
        assertThat(parserOutput.getOutput().stream()
                .filter(m -> !MESSAGE_SOURCE_FILE_STATUS.equals(m.getSource()))
                .count()).isEqualTo(2);

        Files.deleteIfExists(compressedFile);
    }

    private ParserTestUtils.TestParserOutput getFileParserOutput(Path compressedFile) throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidParserException {
        // resolve to real path - MACOS uses links for temp dir
        compressedFile = compressedFile.toRealPath();

        MessageFileParser parser = createMessageFileParserToTest(Collections.singletonList(compressedFile.getParent().toString()));
        MessageToParse messageToParse = MessageFileParserTestUtil.createMessageToParse(compressedFile.toString());
        ParserTestUtils.TestParserOutput parserOutput = new ParserTestUtils.TestParserOutput();
        parser.parse(new ParserChainSource("vpcflow", "netflow", null), messageToParse, parserOutput);

        return parserOutput;
    }

    @Test
    public void testParseZippedFile() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidParserException {
        // Create a temporary zip file
        String sampleContent = "10.0.0.1 10.0.0.2 443 443 6 120 120 162 OK Ingress\n" +
                        "10.0.0.3 10.0.0.4 80 80 6 60 60 162 OK Egress";
        Path tempDirPath = testTempDir.toPath();
        Path zipFile = Files.createTempFile(tempDirPath, "test_data", ".zip");
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
            zipOut.putNextEntry(new ZipEntry("data.txt"));
            zipOut.write(sampleContent.getBytes(StandardCharsets.UTF_8));
            zipOut.closeEntry();
        }

        testCompressedFileParsing(zipFile);
    }

    @Test
    public void testParseGzipExtensionFile() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidParserException {
        // Create a temporary file with .gzip extension (different from .gz)
        String sampleContent = "10.0.0.1 10.0.0.2 443 443 6 120 120 162 OK Ingress\n" +
                        "10.0.0.3 10.0.0.4 80 80 6 60 60 162 OK Egress";
        Path tempDirPath = testTempDir.toPath();
        Path gzipFile = Files.createTempFile(tempDirPath, "test_data", ".gzip");
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(new FileOutputStream(gzipFile.toFile()))) {
            gzipOut.write(sampleContent.getBytes(StandardCharsets.UTF_8));
        }

        testCompressedFileParsing(gzipFile);
    }

    @Test
    public void testSendingInNonCompressedFileWithGzipExtension() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidParserException {
        testBadCompressedFileExtension(".gzip", "java.util.zip.ZipException with message Not in GZIP format");
    }

    @Test
    public void testSendingInNonCompressedFileWithZipExtension() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidParserException {
        testBadCompressedFileExtension(".zip", "java.io.IOException with message ".concat(ZIP_FILE_CONTAINS_NO_ENTRIES_ERROR));
    }

    private void testBadCompressedFileExtension(String extension, String expectedErrorMessage) throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidParserException {
        // Create a temporary file with compressed extension but the file is not actually compressed
        String sampleContent = "10.0.0.1 10.0.0.2 443 443 6 120 120 162 OK Ingress\n" +
                "10.0.0.3 10.0.0.4 80 80 6 60 60 162 OK Egress";
        Path tempDirPath = testTempDir.toPath();
        Path uncompressedFileWithZippedExtension = Files.createTempFile(tempDirPath, "file_with_bad_ext", extension);
        try (FileOutputStream uncompressedFile = new FileOutputStream(uncompressedFileWithZippedExtension.toFile())) {
            uncompressedFile.write(sampleContent.getBytes(StandardCharsets.UTF_8));
        }

        ParserTestUtils.TestParserOutput parserOutput = getFileParserOutput(uncompressedFileWithZippedExtension);
        assertThat(parserOutput.getOutput()).hasSize(1);
        assertThat(parserOutput.getOutput().get(0).getDataQualityMessages().get(0).getMessage()).isEqualTo(expectedErrorMessage);

    }

    @Test
    public void testWithFixedHeaderLineCount() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidParserException {
        // Create a temporary file with header lines
        String headerLine1 = "header1=value1";
        String headerLine2 = "header2=value2";
        String dataLine1 = "10.0.0.1 10.0.0.2 443 443 6 120 120 162 OK Ingress";
        String dataLine2 = "10.0.0.3 10.0.0.4 80 80 6 60 60 162 OK Egress";
        String contentWithHeaders = String.join("\n", Arrays.asList(headerLine1, headerLine2, dataLine1, dataLine2));

        testFileWithHeaders(contentWithHeaders,  new MessageFileHeader(2, null, null), 2);
    }

    private void testFileWithHeaders(String contentWithHeaders, MessageFileHeader messageFileHeader, int expectedHeaderCount) throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidParserException {
        Path tempDirPath = testTempDir.toPath();
        Path fileWithHeaders = Files.createTempFile(tempDirPath, "test_headers", ".txt");
        Files.writeString(fileWithHeaders, contentWithHeaders);

        // resolve to real path
        fileWithHeaders = fileWithHeaders.toRealPath();

        MessageFileParser parser = MessageFileParser.create(
                Collections.singletonList(fileWithHeaders.getParent().toString()),
                createSingleMessageParser());

        MessageToParse messageToParse = MessageFileParserTestUtil.createMessageToParse(fileWithHeaders.toString());
        ParserTestUtils.TestParserOutput parserOutput = new ParserTestUtils.TestParserOutput();
        parser.parse(new ParserChainSource("vpcflow", "netflow", messageFileHeader), messageToParse, parserOutput);

        // Should get 2 data lines (excluding 2 header lines)
        assertThat(parserOutput.getOutput().stream()
                .filter(m -> !MESSAGE_SOURCE_FILE_STATUS.equals(m.getSource()))
                .count()).isEqualTo(2);

        // Verify headerSkipped is in the status extension
        Message statusMessage = parserOutput.getOutput().stream()
                .filter(m -> MESSAGE_SOURCE_FILE_STATUS.equals(m.getSource()))
                .findFirst().orElse(null);
        assertThat(statusMessage).isNotNull();
        assertThat(statusMessage.getExtensions().get("headerLines")).isEqualTo(String.valueOf(expectedHeaderCount));

        Files.deleteIfExists(fileWithHeaders);
    }

    @Test
    public void testWithHeaderPrefixes() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidParserException {
        // Create a temporary file with prefixed header lines
        String headerLine1 = "#header1=value1";
        String headerLine2 = "#header2=value2";
        String dataLine1 = "10.0.0.1 10.0.0.2 443 443 6 120 120 162 OK Ingress";
        String dataLine2 = "10.0.0.3 10.0.0.4 80 80 6 60 60 162 OK Egress";
        String contentWithHeaders = String.join("\n", Arrays.asList(headerLine1, headerLine2, dataLine1, dataLine2));

        testFileWithHeaders(contentWithHeaders, new MessageFileHeader(null, Collections.singletonList("#"), null), 2);
    }

    @Test
    public void testWithHeaderPrefixesAndRequiredHeader() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidParserException {
        // Create a temporary file with prefixed header lines
        String headerLine1 = "#header1=value1";
        String headerLine2 = "#header2=value2";
        String dataLine1 = "10.0.0.1 10.0.0.2 443 443 6 120 120 162 OK Ingress";
        String dataLine2 = "10.0.0.3 10.0.0.4 80 80 6 60 60 162 OK Egress";
        String contentWithHeaders = String.join("\n", Arrays.asList(headerLine1, headerLine2, dataLine1, dataLine2));

        testFileWithHeaders(contentWithHeaders, new MessageFileHeader(null, Collections.singletonList("#"), Collections.singletonList(headerLine1)), 2);
    }

    @Test
    public void testWithRequiredHeaders() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidParserException {
        // Create a temporary file with header lines containing required headers
        String headerLine1 = "requiredHeader=present";
        String dataLine1 = "10.0.0.1 10.0.0.2 443 443 6 120 120 162 OK Ingress";
        String dataLine2 = "10.0.0.3 10.0.0.4 80 80 6 60 60 162 OK Egress";
        String contentWithHeaders = String.join("\n", Arrays.asList(headerLine1, dataLine1, dataLine2));
        testFileWithHeaders(contentWithHeaders, new MessageFileHeader(1, null, Collections.singletonList("requiredHeader=present")), 1);
    }

    @Test
    public void testWithMissingRequiredHeaders() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidParserException {
        // Create a temporary file with header lines but missing required header
        String headerLine1 = "otherHeader=present";
        String dataLine1 = "10.0.0.1 10.0.0.2 443 443 6 120 120 162 OK Ingress";
        String dataLine2 = "10.0.0.3 10.0.0.4 80 80 6 60 60 162 OK Egress";
        String contentWithHeaders = String.join("\n", Arrays.asList(headerLine1, dataLine1, dataLine2));

        testHeaderError(contentWithHeaders, new MessageFileHeader(1, null, Collections.singletonList("requiredHeader")),
                "File is missing required headers: requiredHeader");
    }

    @Test
    public void testNotEnoughHeaderLines() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidParserException {
        // Create a temporary file with header lines but missing required header
        String headerLine1 = "otherHeader=present";

        testHeaderError(headerLine1, new MessageFileHeader(2, null, null),
                "File has 1 lines but required header line count is 2.");
    }

    private void testHeaderError(String contentWithHeaders, MessageFileHeader messageFileHeader, String expectedMessage) throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidParserException {
        Path tempDirPath = testTempDir.toPath();
        Path fileWithHeaders = Files.createTempFile(tempDirPath, "test_missing", ".txt");
        Files.writeString(fileWithHeaders, contentWithHeaders);

        // resolve to real path
        fileWithHeaders = fileWithHeaders.toRealPath();

        MessageFileParser parser = MessageFileParser.create(
                Collections.singletonList(fileWithHeaders.getParent().toString()),
                createSingleMessageParser());

        MessageToParse messageToParse = MessageFileParserTestUtil.createMessageToParse(fileWithHeaders.toString());
        ParserTestUtils.TestParserOutput parserOutput = new ParserTestUtils.TestParserOutput();
        parser.parse(new ParserChainSource("vpcflow", "netflow", messageFileHeader), messageToParse, parserOutput);

        // Should have an error message about missing required header
        assertThat(parserOutput.getOutput()).hasSize(1);
        assertThat(parserOutput.getOutput().get(0).getDataQualityMessages().get(0).getMessage())
                .contains(expectedMessage);

        Files.deleteIfExists(fileWithHeaders);
    }

    @Test
    public void testWithoutHeaderEnabled() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidParserException {
        // Create a temporary file with pretended header lines (no header enabled)
        String headerLine1 = "this looks like a header";
        String headerLine2 = "but its just data";
        String dataLine1 = "10.0.0.1 10.0.0.2 443 443 6 120 120 162 OK Ingress";
        String content = String.join("\n", Arrays.asList(headerLine1, headerLine2, dataLine1));

        Path tempDirPath = testTempDir.toPath();
        Path file = Files.createTempFile(tempDirPath, "test_no_header", ".txt");
        Files.writeString(file, content);

        // resolve to real path
        file = file.toRealPath();

        MessageFileParser parser = MessageFileParser.create(
                Collections.singletonList(file.getParent().toString()),
                createSingleMessageParser());

        MessageToParse messageToParse = MessageFileParserTestUtil.createMessageToParse(file.toString());
        ParserTestUtils.TestParserOutput parserOutput = new ParserTestUtils.TestParserOutput();
        parser.parse(new ParserChainSource("vpcflow", "netflow", null), messageToParse, parserOutput);

        // Should get all 3 lines since header is not enabled
        assertThat(parserOutput.getOutput().stream()
                .filter(m -> !MESSAGE_SOURCE_FILE_STATUS.equals(m.getSource()))
                .count()).isEqualTo(3);

        Files.deleteIfExists(file);
    }

    @Test
    public void testEmptyFileWithPrefixHeaders() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidParserException {

        Path tempDirPath = testTempDir.toPath();
        Path file = Files.createTempFile(tempDirPath, "test_no_header", ".txt");

        // resolve to real path
        file = file.toRealPath();

        MessageFileParser parser = MessageFileParser.create(
                Collections.singletonList(file.getParent().toString()),
                createSingleMessageParser());

        MessageToParse messageToParse = MessageFileParserTestUtil.createMessageToParse(file.toString());
        ParserTestUtils.TestParserOutput parserOutput = new ParserTestUtils.TestParserOutput();
        parser.parse(new ParserChainSource("vpcflow", "netflow",
                new MessageFileHeader(null, Collections.singletonList("prefix"), null)), messageToParse, parserOutput);

        // Should get all 3 lines since header is not enabled
        assertThat(parserOutput.getOutput().stream()
                .filter(m -> !MESSAGE_SOURCE_FILE_STATUS.equals(m.getSource()))
                .count()).isEqualTo(0);

        Files.deleteIfExists(file);
    }

    private SingleMessageParser createSingleMessageParser() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidParserException {
        ParserChainMap parserChainMap = ParserTestUtils.readParserChainMap("message_file/VpcFlowChain.json");
        return SingleMessageParser.create(parserChainMap, null);
    }
}
