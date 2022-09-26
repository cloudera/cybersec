/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.parsers;

import org.apache.commons.io.IOUtils;
import org.apache.metron.parsers.interfaces.MessageParserResult;
import org.apache.metron.stellar.common.JSONMapObject;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MultiLineGrokParserTest {

  /**
   * Test that if a byte[] with multiple lines of log is passed in
   * it will be parsed into the correct number of messages.
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testLegacyInterfaceReturnsMultiline() {

    Map<String, Object> parserConfig = new HashMap<>();
    parserConfig.put("grokPath", getGrokPath());
    parserConfig.put("patternLabel", getGrokPatternLabel());
    parserConfig.put("timestampField", getTimestampField());
    parserConfig.put("dateFormat", getDateFormat());
    parserConfig.put("timeFields", getTimeFields());
    parserConfig.put("multiLine", getMultiLine());
    GrokParser grokParser = new GrokParser();
    grokParser.configure(parserConfig);
    grokParser.init();

    Map<String, String> testData = getTestData();
    for (Map.Entry<String, String> e : testData.entrySet()) {
      byte[] rawMessage = e.getKey().getBytes(StandardCharsets.UTF_8);
      Optional<MessageParserResult<JSONMapObject>> resultOptional = grokParser.parseOptionalResult(rawMessage);
      assertNotNull(resultOptional);
      assertTrue(resultOptional.isPresent());
      List<JSONMapObject> parsedList = resultOptional.get().getMessages();
      assertEquals(10, parsedList.size());
    }
  }

  /**
   * Test that if a byte[] with multiple lines of log is passed in
   * it will be parsed into the correct number of messages using the
   * parseOptionalResult call.
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testOptionalResultReturnsMultiline() {

    Map<String, Object> parserConfig = new HashMap<>();
    parserConfig.put("grokPath", getGrokPath());
    parserConfig.put("patternLabel", getGrokPatternLabel());
    parserConfig.put("timestampField", getTimestampField());
    parserConfig.put("dateFormat", getDateFormat());
    parserConfig.put("timeFields", getTimeFields());
    parserConfig.put("multiLine", getMultiLine());

    GrokParser grokParser = new GrokParser();
    grokParser.configure(parserConfig);
    grokParser.init();

    Map<String, String> testData = getTestData();
    for (Map.Entry<String, String> e : testData.entrySet()) {
      byte[] rawMessage = e.getKey().getBytes(StandardCharsets.UTF_8);
      Optional<MessageParserResult<JSONMapObject>> resultOptional = grokParser.parseOptionalResult(rawMessage);
      assertTrue(resultOptional.isPresent());
      Optional<Throwable> throwableOptional = resultOptional.get().getMasterThrowable();
      List<JSONMapObject>  resultList = resultOptional.get().getMessages();
      Map<Object,Throwable> errorMap = resultOptional.get().getMessageThrowables();
      assertFalse(throwableOptional.isPresent());
      assertEquals(0, errorMap.size());
      assertEquals(10, resultList.size());
    }
  }

  @SuppressWarnings("unchecked")
  public Map getTestData() {

    Map testData = new HashMap<String, String>();
    String input;
    try (FileInputStream stream = new FileInputStream(new File("src/test/resources/logData/multi_elb_log.txt"))) {
      input = IOUtils.toString(stream);
    } catch (IOException ioe) {
      throw new IllegalStateException("failed to open file", ioe);
    }
    // not checking values, just that we get the right number of messages
    testData.put(input, "");
    return testData;

  }

  public String getMultiLine() { return "true";}

  public String getGrokPath() {
    return "/sample/patterns/test";
  }

  public String getGrokPatternLabel() {
    return "ELBACCESSLOGS";
  }

  public List<String> getTimeFields() {
    return new ArrayList<String>() {{
      add("timestamp");
    }};
  }

  public String getDateFormat() {
    return "yyyy-MM-dd'T'HH:mm:ss.S'Z'";
  }

  public String getTimestampField() {
    return "timestamp";
  }
}
