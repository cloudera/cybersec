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

package com.cloudera.cyber.enrichment.geocode.impl;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.DataQualityMessageLevel;
import com.cloudera.cyber.enrichment.Enrichment;
import com.cloudera.cyber.enrichment.SingleValueEnrichment;
import com.cloudera.cyber.enrichment.geocode.impl.types.GeoFields;
import com.maxmind.db.Reader;
import com.maxmind.geoip2.DatabaseProvider;
import com.maxmind.geoip2.model.CityResponse;

import java.net.InetAddress;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Looks up the locations of IPv4 or IPv6 addresses in MaxMind GeoLite2 city database and returns
 * the locations for that IP.
 *
 * This class supports both MaxMind GeoIP2 databases (via DatabaseProvider) and IPinfo databases
 * (via the generic Reader from maxmind-db library).
 */
public class IpGeoEnrichment extends MaxMindBase {

    public static final String GEOCODE_FEATURE = "geo";
    public static final String GEOCODE_FAILED_MESSAGE = "Geocode failed '%s'";

    public IpGeoEnrichment(DatabaseProvider database) {
        super(database);
    }

    public IpGeoEnrichment(DatabaseProvider database, Reader maxmindDbReader) {
        super(database, maxmindDbReader);
    }

    /**
     * Constructor that accepts only a maxmind-db Reader.
     * This supports IPinfo and other MMDB databases.
     *
     * @param maxmindDbReader The generic maxmind-db Reader
     */
    public IpGeoEnrichment(Reader maxmindDbReader) {
        super(maxmindDbReader);
    }

    public IpGeoEnrichment(String path) {
        super(path);
    }

    /**
     * Lookup the IP using the generic maxmind-db Reader (supports IPinfo MMDB).
     *
     * @param fieldName The field name to use for enrichment
     * @param ipFieldValue The IP address to look up
     * @param ipInfoFields The list of IPinfo fields to retrieve
     * @param geoEnrichments The map to populate with enrichment values
     * @param qualityMessages The list to add quality messages to
     * 
     * IPinfo field names: city, region, country, lat, lng, postal_code, region_code, timezone, range
     * MaxMind field names: city, region, country_name, latitude, longitude, postal_code, geoname_id
     */
    public void lookupIpInfo(String fieldName, Object ipFieldValue, List<String> ipInfoFields,
                         Map<String, String> geoEnrichments, List<DataQualityMessage> qualityMessages) {
        if (maxmindDbReader == null) {
            throw new IllegalStateException("maxmindDbReader not initialized. Use constructor with maxmind-db Reader.");
        }

        Enrichment enrichment = new SingleValueEnrichment(fieldName, GEOCODE_FEATURE);
        
        if (ipFieldValue instanceof String) {
            String ipValue = (String) ipFieldValue;
            Map<String, Object> result = lookupGeneric(ipValue);
            if (result != null) {
                for (String fieldKey : ipInfoFields) {
                    // Map IPinfo field names to standard enrichment field names
                    String lookupKey = mapIpInfoFieldName(fieldKey);
                    Object value = result.get(lookupKey);
                    if (value != null) {
                        String key = fieldName + "." + GEOCODE_FEATURE + "." + fieldKey;
                        enrichment.enrich(geoEnrichments, key, value.toString());
                    }
                }
            }
        }
    }
    
    /**
     * Maps IPinfo field names to MaxMind field names for compatibility.
     * IPinfo uses: lat, lng, region_code
     * MaxMind uses: latitude, longitude, region
     */
    private String mapIpInfoFieldName(String fieldName) {
        switch (fieldName) {
            case "lat":
                return "latitude";
            case "lng":
                return "longitude";
            case "region_code":
                return "region";
            case "postal":
                return "postal_code";
            default:
                return fieldName;
        }
    }

    /**
     * Lookup the IP in the city database and return the geo location enrichment fields.
     *
     * @param ipFieldValue A valid IPv4 or IPv6 address represented in a string.
     * @param geoFieldSet  Only Geocoding fields specified are returned.
     *                     For example if the IP has a city in the database but city is not included in this set, the city will not be returned.
     */
    private void lookup(Enrichment enrichment, Function<GeoFields, String> nameFunction, Object ipFieldValue, GeoFields[] geoFieldSet, Map<String, String> geoEnrichments, List<DataQualityMessage> qualityMessages) {
        InetAddress ipAddress = convertToIpAddress(enrichment, ipFieldValue, qualityMessages);
        if (ipAddress != null) {
            try {
                Optional<CityResponse> response = database.tryCity(ipAddress);
                response.ifPresent(cityResponse -> Stream.of(geoFieldSet).map(field -> new AbstractMap.SimpleEntry<>(nameFunction.apply(field), field.getFunction().apply(cityResponse))).
                        filter(entry -> Objects.nonNull(entry.getValue())).
                        forEach(entry -> enrichment.enrich(geoEnrichments, entry.getKey(), entry.getValue())));
            } catch (Exception e) {
                enrichment.addQualityMessage(qualityMessages, DataQualityMessageLevel.ERROR, String.format(GEOCODE_FAILED_MESSAGE, e.getMessage()));
            }
        }
    }

    public void lookup(String fieldName, Object ipFieldValue, GeoFields[] geoFieldSet, Map<String, String> geoEnrichments, List<DataQualityMessage> qualityMessages) {
        lookup(SingleValueEnrichment::new, fieldName, ipFieldValue, geoFieldSet, geoEnrichments, qualityMessages);
    }

    public void lookup(BiFunction<String, String, Enrichment> enrichmentBiFunction, String fieldName, Object ipFieldValue, GeoFields[] geoFieldSet, Map<String, String> geoEnrichments, List<DataQualityMessage> qualityMessages) {
        if (ipFieldValue instanceof Collection) {
            Enrichment enrichment = enrichmentBiFunction.apply(fieldName, GEOCODE_FEATURE);
            //noinspection unchecked
            ((Collection<Object>) ipFieldValue).forEach(ip -> lookup(enrichment, GeoFields::getPluralName, ip, geoFieldSet, geoEnrichments, qualityMessages));
        } else if (ipFieldValue != null) {
            lookup(enrichmentBiFunction.apply(fieldName, GEOCODE_FEATURE), GeoFields::getSingularName, ipFieldValue, geoFieldSet, geoEnrichments, qualityMessages);
        }
    }

}
