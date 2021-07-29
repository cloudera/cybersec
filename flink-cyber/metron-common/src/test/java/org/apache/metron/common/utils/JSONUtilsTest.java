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
import org.apache.metron.test.utils.UnitTestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

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
    tmpDir = UnitTestHelper.createTempDir(new File("target/jsonutilstest"));
    configFile = UnitTestHelper.write(new File(tmpDir, "config.json"), config);
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

}
