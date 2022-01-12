/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.common.utils;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class TestUtils {
  public static long MAX_ASSERT_WAIT_MS = 30000L;

  public interface Assertion {
    void apply() throws Exception;
  }

  public static void assertEventually(Assertion assertion) throws Exception {
    assertEventually(assertion, MAX_ASSERT_WAIT_MS);
  }

  public static void assertEventually(Assertion assertion, long msToWait) throws Exception {
    long delta = msToWait/10;
    for(int i = 0;i < 10;++i) {
      try{
        assertion.apply();
        return;
      }
      catch(AssertionError t) {
      }
      Thread.sleep(delta);
    }
    assertion.apply();
  }

  public static List<byte[]> readSampleData(String samplePath) throws IOException {
    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(samplePath), StandardCharsets.UTF_8));
    List<byte[]> ret = new ArrayList<>();
    for (String line = null; (line = br.readLine()) != null; ) {
      ret.add(line.getBytes(StandardCharsets.UTF_8));
    }
    br.close();
    return ret;
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

  public static String findDir(String name) {
    return findDir(new File("."), name);
  }

  public static String findDir(File startDir, String name) {
    Stack<File> s = new Stack<>();
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

  public static void setLog4jLevel(Class clazz, Level level) {
    Logger logger = Logger.getLogger(clazz);
    logger.setLevel(level);
  }

  public static Level getLog4jLevel(Class clazz) {
    Logger logger = Logger.getLogger(clazz);
    return logger.getLevel();
  }

  /**
   * Cleans up after test run via runtime shutdown hooks
   */
  public static File createTempDir(String prefix) throws IOException {
    final Path tmpDir = Files.createTempDirectory(prefix);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        try {
          cleanDir(tmpDir);
        } catch (IOException e) {
          System.out.println("Warning: Unable to clean tmp folder.");
        }
      }

    });
    return tmpDir.toFile();
  }

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
  }

  public static File createDir(File parent, String child) {
    File newDir = new File(parent, child);
    newDir.mkdirs();
    return newDir;
  }

}
