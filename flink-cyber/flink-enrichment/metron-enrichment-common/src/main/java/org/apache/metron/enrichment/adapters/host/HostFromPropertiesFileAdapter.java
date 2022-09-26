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
import org.apache.metron.stellar.common.JSONMapObject;

import java.util.Map;

@SuppressWarnings("serial")
public class HostFromPropertiesFileAdapter extends AbstractHostAdapter {
	
	Map<String, JSONMapObject> _known_hosts;
	
	public HostFromPropertiesFileAdapter(Map<String, JSONMapObject> known_hosts)
	{
		_known_hosts = known_hosts;
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
	public String getOutputPrefix(CacheKey value) {
		return value.getField();
	}

	@Override
	public void logAccess(CacheKey value) {

	}

	@SuppressWarnings("unchecked")
    @Override
	public JSONMapObject enrich(CacheKey metadata) {
		
		
		if(!_known_hosts.containsKey(metadata.getValue()))
			return new JSONMapObject();

		JSONMapObject enrichment = new JSONMapObject();
		enrichment.put("known_info", _known_hosts.get(metadata.getValue()));
		return enrichment;
	}
	
	
}
