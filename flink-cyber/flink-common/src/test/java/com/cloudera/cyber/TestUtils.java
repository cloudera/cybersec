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

package com.cloudera.cyber;

import com.cloudera.cyber.parser.MessageToParse;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Stack;

import static java.lang.String.format;

public class TestUtils {

    public static String findDir(String name) {
        return findDir(new File("."), name);
    }

    public static String findDir(File startDir, String name) {
        Stack<File> s = new Stack<File>();
        s.push(startDir);
        while (!s.empty()) {
            File parent = s.pop();
            if (parent.getName().equalsIgnoreCase(name)) {
                return parent.getAbsolutePath();
            } else {
                File[] children = parent.listFiles();
                if (children != null) {
                    for (File child : children) {
                        s.push(child);
                    }
                }
            }
        }
        return null;
    }

    public static SignedSourceKey source(String topic, int partition, long offset) {
        return SignedSourceKey.builder()
                .topic(topic)
                .partition(partition)
                .offset(offset)
                .signature(new byte[128])
                .build();
    }

    public static SignedSourceKey source() {
        return source("test", 0, 0);
    }

    public static Message createMessage(Map<String, String> extensions) {
        return createMessage(MessageUtils.getCurrentTimestamp(), "test", extensions);
    }

    public static Message createMessage(long timestamp, String source, Map<String, String> extensions) {
        return Message.builder()
                .ts(timestamp)
                .source(source)
                .extensions(extensions)
                .message("")
                .originalSource(
                        source("topic", 0, 0)).
                        build();
    }

    public static Message createMessage() {
        return createMessage(null);
    }

    public static MessageToParse.MessageToParseBuilder createMessageToParse(String source) {
        return createMessageToParse(source, "test");
    }

    public static MessageToParse.MessageToParseBuilder createMessageToParse(String source, String topic) {
        return MessageToParse.builder()
                .originalBytes(source.getBytes(StandardCharsets.UTF_8))
                .topic(topic)
                .offset(0)
                .partition(0);
    }

    public static SignedSourceKey createOriginal(String topic) {
        return SignedSourceKey.builder().topic(topic).offset(0).partition(0).signature(new byte[128]).build();
    }
    public static SignedSourceKey createOriginal() {
        return createOriginal("test");
    }

    /**
     * Create directory that is automatically cleaned up after the
     * JVM shuts down through use of a Runtime shutdown hook.
     *
     * @param dir Directory to create, including missing parent directories
     * @return File handle to the created temp directory
     */
    public static File createTempDir(File dir) throws IOException {
        return createTempDir(dir, true);
    }

    /**
     * Create directory that is automatically cleaned up after the
     * JVM shuts down through use of a Runtime shutdown hook.
     *
     * @param dir Directory to create, including missing parent directories
     * @param cleanup true/false
     * @return File handle to the created temp directory
     */
    public static File createTempDir(File dir, boolean cleanup) throws IOException {
        if (!dir.mkdirs() && !dir.exists()) {
            throw new IOException(String.format("Failed to create directory structure '%s'", dir.toString()));
        }
        if (cleanup) {
            addCleanupHook(dir.toPath());
        }
        return dir;
    }

    /**
     * Create directory that is automatically cleaned up after the
     * JVM shuts down through use of a Runtime shutdown hook.
     *
     * @param prefix Prefix to apply to temp directory name
     * @return File handle to the created temp directory
     * @throws IOException Unable to create temp directory
     */
    public static File createTempDir(String prefix) throws IOException {
        return createTempDir(prefix, true);
    }

    /**
     * Create directory that is optionally cleaned up after the
     * JVM shuts down through use of a Runtime shutdown hook.
     *
     * @param prefix Prefix to apply to temp directory name
     * @param cleanup true/false
     * @return File handle to the created temp directory
     * @throws IOException Unable to create temp directory
     */
    public static File createTempDir(String prefix, boolean cleanup) throws IOException {
        Path tmpDir = Files.createTempDirectory(prefix);
        addCleanupHook(tmpDir);
        return tmpDir.toFile();
    }

    /**
     * Adds JVM shutdown hook that will recursively delete the passed directory
     *
     * @param dir Directory that will be cleaned up upon JVM shutdown
     */
    public static void addCleanupHook(final Path dir) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    cleanDir(dir);
                } catch (IOException e) {
                    System.out.println(format("Warning: Unable to clean folder '%s'", dir.toString()));
                }
            }
        });
    }

    /**
     * Returns file passed in after writing
     *
     * @param file
     * @param contents
     * @return
     * @throws IOException
     */
    public static File write(File file, String contents) throws IOException {
        com.google.common.io.Files.createParentDirs(file);
        com.google.common.io.Files.write(contents, file, StandardCharsets.UTF_8);
        return file;
    }

    public static void write(File file, String[] contents) throws IOException {
        StringBuilder b = new StringBuilder();
        for (String line : contents) {
            b.append(line);
            b.append(System.lineSeparator());
        }
        write(file, b.toString());
    }

    /**
     * Reads file contents into a String. Uses UTF-8 as default charset.
     *
     * @param in Input file
     * @return contents of input file
     * @throws IOException
     */
    public static String read(File in) throws IOException {
        return read(in, StandardCharsets.UTF_8);
    }

    /**
     * Reads file contents into a String
     *
     * @param in Input file
     * @param charset charset to use for reading
     * @return contents of input file
     * @throws IOException
     */
    public static String read(File in, Charset charset) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(in.getPath()));
        return new String(bytes, charset);
    }


    /**
     * Recursive directory delete
     *
     * @param dir Directory to delete
     * @throws IOException Unable to delete
     */
    public static void cleanDir(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc == null) {
                    return FileVisitResult.CONTINUE;
                } else {
                    throw exc;
                }
            }
        });
        Files.delete(dir);
    }
}
