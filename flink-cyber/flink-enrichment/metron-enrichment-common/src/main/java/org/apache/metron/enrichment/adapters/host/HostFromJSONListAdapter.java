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

import org.apache.metron.enrichment.cache.CacheKey;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HostFromJSONListAdapter extends AbstractHostAdapter {

  Map<String, JSONObject> _known_hosts = new HashMap<>();

  public HostFromJSONListAdapter(String jsonList) {
    JSONArray jsonArray = (JSONArray) JSONValue.parse(jsonList);
    Iterator jsonArrayIterator = jsonArray.iterator();
    while(jsonArrayIterator.hasNext()) {
      JSONObject jsonObject = (JSONObject) jsonArrayIterator.next();
      String host = (String) jsonObject.remove("ip");
      _known_hosts.put(host, jsonObject);
    }
  }

  @Override
	public String getOutputPrefix(CacheKey value) {
		return value.getField();
	}

  @Override
  public boolean initializeAdapter(Map<String, Object> config)
  {

    if(_known_hosts.size() > 0)
      return true;
    else
      return false;
  }

  @Override
  public void updateAdapter(Map<String, Object> config) {
  }

  @Override
  public void logAccess(CacheKey value) {

  }

  @SuppressWarnings("unchecked")
  @Override
  public JSONObject enrich(CacheKey k) {
    String metadata = k.coerceValue(String.class);

    if(!_known_hosts.containsKey(metadata))
      return new JSONObject();

    JSONObject enrichment = new JSONObject();
    String prefix = "known_info.";
    JSONObject knownInfo = _known_hosts.get(metadata);
    for(Object key: knownInfo.keySet()) {
      enrichment.put(prefix + key, knownInfo.get(key));
    }
    //enrichment.put("known_info", _known_hosts.get(metadata));
    return enrichment;
  }
}
