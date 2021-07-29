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
package org.apache.metron.enrichment.adapters.threatintel;

import com.google.common.collect.Iterables;
import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.metron.enrichment.cache.CacheKey;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;
import org.apache.metron.enrichment.lookup.EnrichmentLookup;
import org.apache.metron.enrichment.lookup.accesstracker.BloomAccessTracker;
import org.apache.metron.enrichment.lookup.accesstracker.PersistentAccessTracker;
import org.apache.metron.enrichment.utils.EnrichmentUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreatIntelAdapter implements EnrichmentAdapter<CacheKey>,Serializable {
  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  protected ThreatIntelConfig config;
  protected EnrichmentLookup lookup;
  protected Connection connection;

  public ThreatIntelAdapter() {
  }
  public ThreatIntelAdapter(ThreatIntelConfig config) {
    withConfig(config);
  }

  public ThreatIntelAdapter withConfig(ThreatIntelConfig config) {
    this.config = config;
    return this;
  }

  @Override
  public void logAccess(CacheKey value) {
    List<String> enrichmentTypes = value.getConfig().getThreatIntel().getFieldToTypeMap().get(value.getField());
    if(enrichmentTypes != null) {
      for(String enrichmentType : enrichmentTypes) {
        lookup.getAccessTracker().logAccess(new EnrichmentKey(enrichmentType, value.coerceValue(String.class)));
      }
    }
  }


  @Override
  public JSONObject enrich(CacheKey value) {
    if(!isInitialized()) {
      initializeAdapter(null);
    }
    JSONObject enriched = new JSONObject();
    List<String> enrichmentTypes = value.getConfig()
                                        .getThreatIntel().getFieldToTypeMap()
                                        .get(EnrichmentUtils.toTopLevelField(value.getField()));
    if(isInitialized() && enrichmentTypes != null) {
      int i = 0;
      try {
        for (Boolean isThreat :
                lookup.exists(Iterables.transform(enrichmentTypes
                                                 , new EnrichmentUtils.TypeToKey(value.coerceValue(String.class)
                                                                                , lookup.getTable()
                                                                                , value.getConfig().getThreatIntel()
                                                                                )
                                                 )
                             , false
                             )
            )
        {
          String enrichmentType = enrichmentTypes.get(i++);
          if (isThreat) {
            enriched.put(enrichmentType, "alert");
            LOG.trace("Theat Intel Enriched value => {}", enriched);
          }
        }
      }
      catch(IOException e) {
        LOG.error("Unable to retrieve value: {}", e.getMessage(), e);
        initializeAdapter(null);
        throw new RuntimeException("Theat Intel Unable to retrieve value", e);
      }
    }
    LOG.trace("Threat Intel Enrichment Success: {}", enriched);
    return enriched;
  }

  public boolean isInitialized() {
    return lookup != null && lookup.getTable() != null;
  }

  @Override
  public boolean initializeAdapter(Map<String, Object> configuration) {
    PersistentAccessTracker accessTracker;
    String hbaseTable = config.getHBaseTable();
    int expectedInsertions = config.getExpectedInsertions();
    double falsePositives = config.getFalsePositiveRate();
    String trackerHBaseTable = config.getTrackerHBaseTable();
    String trackerHBaseCF = config.getTrackerHBaseCF();
    long millisecondsBetweenPersist = config.getMillisecondsBetweenPersists();
    BloomAccessTracker bat = new BloomAccessTracker(hbaseTable, expectedInsertions, falsePositives);
    Configuration hbaseConfig = HBaseConfiguration.create();
    try {
      accessTracker = new PersistentAccessTracker( hbaseTable
              , UUID.randomUUID().toString()
              , config.getProvider().getTable(hbaseConfig, trackerHBaseTable)
              , trackerHBaseCF
              , bat
              , millisecondsBetweenPersist
      );
      lookup = new EnrichmentLookup(config.getProvider().getTable(hbaseConfig, hbaseTable), config.getHBaseCF(), accessTracker);
    } catch (IOException e) {
      LOG.error("Unable to initialize ThreatIntelAdapter", e);
      return false;
    }

    return true;
  }

  @Override
  public void updateAdapter(Map<String, Object> config) {
  }

  @Override
  public void cleanup() {
    try {
      lookup.close();
    } catch (Exception e) {
      throw new RuntimeException("Unable to cleanup access tracker", e);
    }
  }

  @Override
  public String getOutputPrefix(CacheKey value) {
    return value.getField();
  }
}
