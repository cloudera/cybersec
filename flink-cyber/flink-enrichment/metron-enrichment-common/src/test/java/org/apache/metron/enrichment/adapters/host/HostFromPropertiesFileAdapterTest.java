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
import org.apache.metron.stellar.common.JSONMapObject;
import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class HostFromPropertiesFileAdapterTest {

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
     * "known_info":
     * {"asset_value":"important",
     * "type":"printer","local":"YES"
     * }
     * }
     */
    @Multiline
    private String expectedMessageString;

    private JSONMapObject expectedMessage;
    private String ip = "10.0.2.15";
    private String ip1 = "10.0.22.22";

    @BeforeEach
    public void parseJSON()  {
        expectedMessage = new JSONMapObject(expectedMessageString);
    }

    @Test
    public void testEnrich() {
        Map<String, JSONMapObject> mapKnownHosts = new HashMap<>();
        JSONArray jsonArray = new JSONArray(expectedKnownHostsString);
        Iterator jsonArrayIterator = jsonArray.iterator();
        while(jsonArrayIterator.hasNext()) {
            JSONMapObject jsonObject = (JSONMapObject) jsonArrayIterator.next();
            String host = (String) jsonObject.remove("ip");
            mapKnownHosts.put(host, jsonObject);
        }
        HostFromPropertiesFileAdapter hfa = new HostFromPropertiesFileAdapter(mapKnownHosts);
        JSONMapObject actualMessage = hfa.enrich(new CacheKey("dummy", ip, null));
        assertNotNull(actualMessage);
        assertEquals(expectedMessage, actualMessage);
        actualMessage = hfa.enrich(new CacheKey("dummy", ip1, null));
        JSONMapObject emptyJson = new JSONMapObject();
        assertEquals(emptyJson, actualMessage);
    }


    @Test
    public void testInitializeAdapter() {
        Map<String, JSONMapObject> mapKnownHosts = new HashMap<>();
        HostFromPropertiesFileAdapter hfa = new HostFromPropertiesFileAdapter(mapKnownHosts);
        assertFalse(hfa.initializeAdapter(null));
        JSONArray jsonArray = new JSONArray(expectedKnownHostsString);
        Iterator jsonArrayIterator = jsonArray.iterator();
        while(jsonArrayIterator.hasNext()) {
            JSONMapObject jsonObject = (JSONMapObject) jsonArrayIterator.next();
            String host = (String) jsonObject.remove("ip");
            mapKnownHosts.put(host, jsonObject);
        }
        hfa = new HostFromPropertiesFileAdapter(mapKnownHosts);
        assertTrue(hfa.initializeAdapter(null));
    }

}

