package com.cloudera.cyber.enrichemnt.stellar.adapter;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.enrichment.MetronGeoEnrichment;
import com.cloudera.cyber.enrichment.geocode.IpGeoJob;
import com.cloudera.cyber.enrichment.geocode.impl.IpGeoEnrichment;
import com.cloudera.cyber.enrichment.geocode.impl.types.MetronGeoEnrichmentFields;
import lombok.extern.slf4j.Slf4j;
import org.apache.metron.enrichment.cache.CacheKey;
import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;
import org.apache.metron.stellar.common.JSONMapObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class MetronGeoEnrichmentAdapter implements EnrichmentAdapter<CacheKey>, Serializable {

    private IpGeoEnrichment ipGeoEnrichment;

    @Override
    public JSONMapObject enrich(CacheKey value) {
        JSONMapObject enriched;
        HashMap<String, String> result = new HashMap<>();
        List<DataQualityMessage> qualityMessages = new ArrayList<>();
        ipGeoEnrichment.lookup(MetronGeoEnrichment::new, value.getField(), value.getValue(), MetronGeoEnrichmentFields.values(), result, qualityMessages);
        if (result.isEmpty()) {
            return new JSONMapObject();
        }
        enriched = new JSONMapObject(result);
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
