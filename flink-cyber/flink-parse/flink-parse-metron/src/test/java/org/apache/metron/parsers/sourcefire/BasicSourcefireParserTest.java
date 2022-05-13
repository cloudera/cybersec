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
package org.apache.metron.parsers.sourcefire;

import org.apache.metron.parsers.AbstractParserConfigTest;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BasicSourcefireParserTest extends AbstractParserConfigTest {

  @BeforeEach
  public void setUp() throws Exception {
    inputStrings = super
        .readTestDataFromFile("src/test/resources/logData/SourcefireParserTest.txt");
    parser = new BasicSourcefireParser();
  }

  @SuppressWarnings({"rawtypes", "unused"})
  @Test
  public void testParse() throws ParseException {
    for (String inputString : inputStrings) {
      byte[] srcBytes = inputString.getBytes(StandardCharsets.UTF_8);
      JSONObject parsed = parser.parse(inputString.getBytes(StandardCharsets.UTF_8)).get(0);
      assertNotNull(parsed);

      JSONParser parser = new JSONParser();
      Map json = (Map) parser.parse(parsed.toJSONString());

      for (Object o : json.entrySet()) {
        Entry entry = (Entry) o;
        String key = (String) entry.getKey();
        String value = json.get("original_string").toString();
        assertNotNull(value);
      }
    }
  }

  @Test
  public void getsReadCharsetFromConfig() {
    Map<String, Object> config = new HashMap<>();
    config.put(MessageParser.READ_CHARSET, StandardCharsets.UTF_16.toString());
    parser.configure(config);
    assertThat(parser.getReadCharset(), equalTo(StandardCharsets.UTF_16));
  }

  @Test
  public void getsReadCharsetFromDefault() {
    Map<String, Object> config = new HashMap<>();
    parser.configure(config);
    assertThat(parser.getReadCharset(), equalTo(StandardCharsets.UTF_8));
  }
}
