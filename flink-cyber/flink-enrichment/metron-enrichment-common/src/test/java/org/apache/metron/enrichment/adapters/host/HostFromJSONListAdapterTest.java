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
package org.apache.metron.enrichment.adapters.host;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.enrichment.cache.CacheKey;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HostFromJSONListAdapterTest {


  /**
   * [
   * {"ip":"10.1.128.236", "local":"YES", "type":"webserver", "asset_value" : "important"},
   * {"ip":"10.1.128.237", "local":"UNKNOWN", "type":"unknown", "asset_value" : "important"},
   * {"ip":"10.60.10.254", "local":"YES", "type":"printer", "asset_value" : "important"},
   * {"ip":"10.0.2.15", "local":"YES", "type":"printer", "asset_value" : "important"}
   * ]
   */
  @Multiline
  private String expectedKnownHostsString;

  /**
   * {
   * "known_info.local":"YES",
   * "known_info.type":"printer",
   * "known_info.asset_value" : "important"
   * }
   */
  @Multiline
  private String expectedMessageString;

  private JSONObject expectedMessage;
  private String ip = "10.0.2.15";
  private String ip1 = "10.0.22.22";


  @BeforeEach
  public void parseJSON() throws ParseException {
    JSONParser jsonParser = new JSONParser();
    expectedMessage = (JSONObject) jsonParser.parse(expectedMessageString);
  }

  @Test
  public void testEnrich() {
    HostFromJSONListAdapter hja = new HostFromJSONListAdapter(expectedKnownHostsString);
    JSONObject actualMessage = hja.enrich(new CacheKey("dummy", ip, null));
    assertNotNull(actualMessage);
    assertEquals(expectedMessage, actualMessage);
    actualMessage = hja.enrich(new CacheKey("dummy", ip1, null));
    JSONObject emptyJson = new JSONObject();
    assertEquals(emptyJson, actualMessage);
  }

  @Test
  public void testEnrichNonString() {
    HostFromJSONListAdapter hja = new HostFromJSONListAdapter(expectedKnownHostsString);
    JSONObject actualMessage = hja.enrich(new CacheKey("dummy", ip, null));
    assertNotNull(actualMessage);
    assertEquals(expectedMessage, actualMessage);
    actualMessage = hja.enrich(new CacheKey("dummy", 10L, null));
    JSONObject emptyJson = new JSONObject();
    assertEquals(emptyJson, actualMessage);
  }
  @Test
  public void testInitializeAdapter() {
    HostFromJSONListAdapter hja = new HostFromJSONListAdapter(expectedKnownHostsString);
    assertTrue(hja.initializeAdapter(null));
  }

}

