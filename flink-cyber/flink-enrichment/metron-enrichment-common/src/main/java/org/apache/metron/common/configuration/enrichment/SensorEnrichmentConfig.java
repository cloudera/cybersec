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
package org.apache.metron.common.configuration.enrichment;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.nio.charset.StandardCharsets;
import org.apache.metron.common.configuration.enrichment.threatintel.ThreatIntelConfig;
import org.apache.metron.common.utils.JSONUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Allows for retrieval and update of enrichment configurations.
 */
public class SensorEnrichmentConfig {

  private EnrichmentConfig enrichment = new EnrichmentConfig();
  private ThreatIntelConfig threatIntel = new ThreatIntelConfig();
  private Map<String, Object> configuration = new HashMap<>();

  public Map<String, Object> getConfiguration() {
    return configuration;
  }

  public void setConfiguration(Map<String, Object> configuration) {
    this.configuration = configuration;
  }

  public EnrichmentConfig getEnrichment() {
    return enrichment;
  }

  public void setEnrichment(EnrichmentConfig enrichment) {
    this.enrichment = enrichment;
  }

  public ThreatIntelConfig getThreatIntel() {
    return threatIntel;
  }

  public void setThreatIntel(ThreatIntelConfig threatIntel) {
    this.threatIntel = threatIntel;
  }



  @Override
  public String toString() {
    return "SensorEnrichmentConfig{" +
            ", enrichment=" + enrichment +
            ", threatIntel=" + threatIntel +
            ", configuration=" + configuration +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SensorEnrichmentConfig that = (SensorEnrichmentConfig) o;

    if (getEnrichment() != null ? !getEnrichment().equals(that.getEnrichment()) : that.getEnrichment() != null)
      return false;
    if (getThreatIntel() != null ? !getThreatIntel().equals(that.getThreatIntel()) : that.getThreatIntel() != null)
      return false;
    return getConfiguration() != null ? getConfiguration().equals(that.getConfiguration()) : that.getConfiguration() == null;

  }

  @Override
  public int hashCode() {
    int result = getEnrichment() != null ? getEnrichment().hashCode() : 0;
    result = 31 * result + (getEnrichment() != null ? getEnrichment().hashCode() : 0);
    result = 31 * result + (getThreatIntel() != null ? getThreatIntel().hashCode() : 0);
    result = 31 * result + (getConfiguration() != null ? getConfiguration().hashCode() : 0);
    return result;
  }

  public static SensorEnrichmentConfig fromBytes(byte[] config) throws IOException {
    return JSONUtils.INSTANCE.load(new String(config, StandardCharsets.UTF_8), SensorEnrichmentConfig.class);
  }

  public String toJSON() throws JsonProcessingException {
    return JSONUtils.INSTANCE.toJSON(this, true);
  }

}
