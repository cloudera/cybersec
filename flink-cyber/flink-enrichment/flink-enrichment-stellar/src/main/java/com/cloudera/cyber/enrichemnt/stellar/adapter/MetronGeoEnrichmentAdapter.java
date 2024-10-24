/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

package com.cloudera.cyber.enrichemnt.stellar.adapter;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.enrichment.MetronGeoEnrichment;
import com.cloudera.cyber.enrichment.geocode.IpGeoJob;
import com.cloudera.cyber.enrichment.geocode.impl.IpGeoEnrichment;
import com.cloudera.cyber.enrichment.geocode.impl.types.MetronGeoEnrichmentFields;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.metron.enrichment.cache.CacheKey;
import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;
import org.json.simple.JSONObject;

@Slf4j
public class MetronGeoEnrichmentAdapter implements EnrichmentAdapter<CacheKey>, Serializable {

    private IpGeoEnrichment ipGeoEnrichment;

    @Override
    public JSONObject enrich(CacheKey value) {
        JSONObject enriched;
        HashMap<String, String> result = new HashMap<>();
        List<DataQualityMessage> qualityMessages = new ArrayList<>();
        ipGeoEnrichment.lookup(MetronGeoEnrichment::new, value.getField(), value.getValue(),
              MetronGeoEnrichmentFields.values(), result, qualityMessages);
        if (result.isEmpty()) {
            return new JSONObject();
        }
        enriched = new JSONObject(result);
        log.trace("GEO Enrichment success: {}", enriched);
        return enriched;
    }

    @Override
    public boolean initializeAdapter(Map<String, Object> config) {
        log.info("Initializing GeoEnrichmentFunctions");
        String hdfsDir = (String) config.get(IpGeoJob.PARAM_GEO_DATABASE_PATH);
        ipGeoEnrichment = new IpGeoEnrichment(hdfsDir);
        return true;
    }

    @Override
    public String getOutputPrefix(CacheKey value) {
        return value.getField();
    }

    @Override
    public void updateAdapter(Map<String, Object> config) {

    }

    @Override
    public void logAccess(CacheKey value) {
    }


    @Override
    public void cleanup() {

    }
}
