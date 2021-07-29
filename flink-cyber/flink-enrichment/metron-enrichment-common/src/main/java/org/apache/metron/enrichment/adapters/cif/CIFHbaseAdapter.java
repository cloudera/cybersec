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

package org.apache.metron.enrichment.adapters.cif;

import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.metron.enrichment.cache.CacheKey;
import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unchecked")
public class CIFHbaseAdapter implements EnrichmentAdapter<CacheKey>,Serializable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final long serialVersionUID = 1L;
	private String _tableName;
	private Table table;
	private String _quorum;
	private String _port;

	public CIFHbaseAdapter(String quorum, String port, String tableName) {
		_quorum = quorum;
		_port = port;
		_tableName = tableName;
	}


	@Override
	public void logAccess(CacheKey value) {

	}

	@Override
	public JSONObject enrich(CacheKey k) {
		String metadata = k.coerceValue(String.class);
		JSONObject output = new JSONObject();
		LOGGER.debug("=======Looking Up For: {}", metadata);
		output.putAll(getCIFObject(metadata));

		return output;
	}

	@SuppressWarnings({ "rawtypes", "deprecation" })
	protected Map getCIFObject(String key) {

		LOGGER.debug("=======Pinging HBase For: {}", key);

		Get get = new Get(key.getBytes(StandardCharsets.UTF_8));
		Result rs;
		Map output = new HashMap();

		try {
			rs = table.get(get);

			for (KeyValue kv : rs.raw())
				output.put(new String(kv.getQualifier(), StandardCharsets.UTF_8), "Y");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return output;
	}

	@Override
	public boolean initializeAdapter(Map<String, Object> config) {

		// Initialize HBase Table
		Configuration conf = null;
		conf = HBaseConfiguration.create();
		conf.set("hbase.zookeeper.quorum", _quorum);
		conf.set("hbase.zookeeper.property.clientPort", _port);

		try {
			LOGGER.debug("=======Connecting to HBASE===========");
			LOGGER.debug("=======ZOOKEEPER = {}", conf.get("hbase.zookeeper.quorum"));
			Connection connection = ConnectionFactory.createConnection(conf);
			table = connection.getTable(TableName.valueOf(_tableName));
			return true;
		} catch (IOException e) {
			LOGGER.debug("=======Unable to Connect to HBASE===========");
			e.printStackTrace();
		}

		return false;
	}

	@Override
	public void updateAdapter(Map<String, Object> config) {
	}


	public String enrichByIP(String metadata) {
		return null;
	}


	public String enrichByDomain(String metadata) {
		return null;
	}


	public String enrichByEmail(String metadata) {
		return null;
	}

	@Override
	public void cleanup() {

	}

	@Override
	public String getOutputPrefix(CacheKey value) {
		return value.getField();
	}
}
