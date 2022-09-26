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
package org.apache.metron.parsers;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import org.apache.metron.parsers.interfaces.MessageParserResult;
import org.apache.metron.stellar.common.JSONMapObject;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public abstract class GrokParserTest {

  @Test
  public void test() throws ParseException, org.json.simple.parser.ParseException {

    Map<String, Object> parserConfig = new HashMap<>();
    parserConfig.put("grokPath", getGrokPath());
    parserConfig.put("patternLabel", getGrokPatternLabel());
    parserConfig.put("timestampField", getTimestampField());
    parserConfig.put("dateFormat", getDateFormat());
    parserConfig.put("timeFields", getTimeFields());

    GrokParser grokParser = new GrokParser();
    grokParser.configure(parserConfig);
    grokParser.init();

    Map<String,String> testData = getTestData();
    for( Map.Entry<String,String> e : testData.entrySet() ){

      JSONMapObject expected = new JSONMapObject(e.getValue());
      byte[] rawMessage = e.getKey().getBytes(StandardCharsets.UTF_8);
      Optional<MessageParserResult<JSONMapObject>> resultOptional = grokParser.parseOptionalResult(rawMessage);
      assertNotNull(resultOptional);
      assertTrue(resultOptional.isPresent());
      List<JSONMapObject> parsedList = resultOptional.get().getMessages();
      assertEquals(1, parsedList.size());
      compare(expected, parsedList.get(0));
    }

  }

  public boolean compare(JSONMapObject expected, JSONMapObject actual) {
    MapDifference mapDifferences = Maps.difference(expected, actual);
    if (mapDifferences.entriesOnlyOnLeft().size() > 0) {
      fail("Expected JSON has extra parameters: " + mapDifferences.entriesOnlyOnLeft());
    }
    if (mapDifferences.entriesOnlyOnRight().size() > 0) {
      fail("Actual JSON has extra parameters: " + mapDifferences.entriesOnlyOnRight());
    }
    Map actualDifferences = new HashMap();
    if (mapDifferences.entriesDiffering().size() > 0) {
      Map differences = Collections.unmodifiableMap(mapDifferences.entriesDiffering());
      for (Object key : differences.keySet()) {
        Object expectedValueObject = expected.get(key);
        Object actualValueObject = actual.get(key);
        if (expectedValueObject instanceof Long || expectedValueObject instanceof Integer) {
          Long expectedValue = Long.parseLong(expectedValueObject.toString());
          Long actualValue = Long.parseLong(actualValueObject.toString());
          if (!expectedValue.equals(actualValue)) {
            actualDifferences.put(key, differences.get(key));
          }
        } else {
          actualDifferences.put(key, differences.get(key));
        }
      }
    }
    if (actualDifferences.size() > 0) {
      fail("Expected and Actual JSON values don't match: " + actualDifferences);
    }
    return true;
  }

  public abstract Map getTestData();
  public abstract String getGrokPath();
  public abstract String getGrokPatternLabel();
  public abstract List<String> getTimeFields();
  public abstract String getDateFormat();
  public abstract String getTimestampField();
  public abstract String getMultiLine();
}
