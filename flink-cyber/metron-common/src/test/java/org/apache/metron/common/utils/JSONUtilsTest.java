/*
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

import org.adrianwalker.multilinestring.Multiline;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class JSONUtilsTest {

  private static File tmpDir;

  /**
   * { "a" : "hello", "b" : "world" }
   */
  @Multiline
  private static String config;
  private static File configFile;

  @BeforeAll
  public static void setUp() throws Exception {
    tmpDir = createTempDir(new File("target/jsonutilstest"));
    configFile = writeToFile(new File(tmpDir, "config.json"), config);
  }

  @Test
  public void loads_file_with_typeref() throws Exception {
    Map<String, Object> expected = new HashMap<String, Object>() {{
      put("a", "hello");
      put("b", "world");
    }};
    Map<String, Object> actual = JSONUtils.INSTANCE
        .load(configFile, JSONUtils.MAP_SUPPLIER);
    assertThat("config not equal", actual, equalTo(expected));
  }

  @Test
  public void loads_file_with_map_class() throws Exception {
    Map<String, Object> expected = new HashMap<String, Object>() {{
      put("a", "hello");
      put("b", "world");
    }};
    Map<String, Object> actual = JSONUtils.INSTANCE.load(configFile, Map.class);
    assertThat("config not equal", actual, equalTo(expected));
  }

  @Test
  public void loads_file_with_custom_class() throws Exception {
    TestConfig expected = new TestConfig().setA("hello").setB("world");
    TestConfig actual = JSONUtils.INSTANCE.load(configFile, TestConfig.class);
    assertThat("a not equal", actual.getA(), equalTo(expected.getA()));
    assertThat("b not equal", actual.getB(), equalTo(expected.getB()));
  }

  public static class TestConfig {

    private String a;
    private String b;

    public String getA() {
      return a;
    }

    public TestConfig setA(String a) {
      this.a = a;
      return this;
    }

    public String getB() {
      return b;
    }

    public TestConfig setB(String b) {
      this.b = b;
      return this;
    }
  }

  /**
   * { "a": "b" }
   */
  @Multiline
  public static String sourceJson;

  /**
   * [{ "op": "move", "from": "/a", "path": "/c" }]
   */
  @Multiline
  public static String patchJson;

  /**
   * { "c": "b" }
   */
  @Multiline
  public static String expectedJson;

  @Test
  public void applyPatch_modifies_source_json_doc() throws IOException {
    String actual = new String(JSONUtils.INSTANCE.applyPatch(patchJson, sourceJson),
        StandardCharsets.UTF_8);
    assertThat(JSONUtils.INSTANCE.load(actual, JSONUtils.MAP_SUPPLIER), equalTo(JSONUtils.INSTANCE.load(expectedJson, JSONUtils.MAP_SUPPLIER)));
  }

  /**
   * {
   *    "foo" : {
   *      "bar" : {
   *        "baz" : [ "val1", "val2" ]
   *      }
   *    }
   * }
   */
  @Multiline
  public static String complexJson;

  /**
   * [{ "op": "add", "path": "/foo/bar/baz", "value": [ "new1", "new2" ] }]
   */
  @Multiline
  public static String patchComplexJson;

  /**
   * {
   *    "foo" : {
   *      "bar" : {
   *        "baz" : [ "new1", "new2" ]
   *      }
   *    }
   * }
   */
  @Multiline
  public static String expectedComplexJson;

  @Test
  public void applyPatch_modifies_complex_source_json_doc() throws IOException {
    String actual = new String(JSONUtils.INSTANCE.applyPatch(patchComplexJson, complexJson),
        StandardCharsets.UTF_8);
    assertThat(JSONUtils.INSTANCE.load(actual, JSONUtils.MAP_SUPPLIER), equalTo(JSONUtils.INSTANCE.load(expectedComplexJson, JSONUtils.MAP_SUPPLIER)));
  }

  public static File createTempDir(File dir) throws IOException {
    if (!dir.mkdirs() && !dir.exists()) {
      throw new IOException(String.format("Failed to create directory structure '%s'", dir.toString()));
    }
    addCleanupHook(dir.toPath());
    return dir;
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

  /**
   * Write contents to a file
   *
   * @param file
   * @param contents
   * @return file handle
   * @throws IOException
   */
  public static File writeToFile(File file, String contents) throws IOException {
    com.google.common.io.Files.createParentDirs(file);
    com.google.common.io.Files.write(contents, file, StandardCharsets.UTF_8);
    return file;
  }

}
