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

import java.io.Serializable;
import java.util.Map;

import org.apache.metron.enrichment.cache.CacheKey;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;

public abstract class AbstractHostAdapter implements EnrichmentAdapter<CacheKey>,
				Serializable{

	/**
	 * Adapter to attach reputation information to the telemetry message
	 */
	private static final long serialVersionUID = 8280523289446309728L;
	protected static final Logger LOG = LoggerFactory
			.getLogger(AbstractHostAdapter.class);
	
	@Override
	abstract public boolean initializeAdapter(Map<String, Object> config);
	@Override
	abstract public JSONObject enrich(CacheKey metadata);

	@Override
	public void cleanup() {

	}
}
