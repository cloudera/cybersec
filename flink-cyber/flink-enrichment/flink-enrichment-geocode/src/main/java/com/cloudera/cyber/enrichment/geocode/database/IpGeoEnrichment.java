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

package com.cloudera.cyber.enrichment.geocode.database;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.DataQualityMessageLevel;
import com.cloudera.cyber.enrichment.Enrichment;
import com.cloudera.cyber.enrichment.SingleValueEnrichment;
import com.cloudera.cyber.enrichment.geocode.database.types.GeoFields;
import com.cloudera.cyber.enrichment.geocode.database.types.geo.GeoDatabase;

import java.io.IOException;
import java.net.InetAddress;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Looks up the locations of IPv4 or IPv6 addresses in MaxMind GeoLite2 city database and returns
 * the locations for that IP.
 */
public class IpGeoEnrichment extends IpEnrichment {

    public static final String GEOCODE_FEATURE = "geo";
    public static final String GEOCODE_FAILED_MESSAGE = "Geocode failed '%s'";
    GeoDatabase database;

    public IpGeoEnrichment(String path) {
       this.database = new GeoDatabase(path);
    }

    /**
     * Lookup the IP in the city database and return the geolocation enrichment fields.
     *
     * @param ipFieldValue A valid IPv4 or IPv6 address represented in a string.
     * @param geoFieldSet  Only Geocoding fields specified are returned.
     *                     For example if the IP has a city in the database but city is not included in this set, the city will not be returned.
     */
    private void lookup(Enrichment enrichment, Function<GeoFields, String> nameFunction, Object ipFieldValue, GeoFields[] geoFieldSet, Map<String, String> geoEnrichments, List<DataQualityMessage> qualityMessages) {
        InetAddress ipAddress = convertToIpAddress(enrichment, ipFieldValue, qualityMessages);
        if (ipAddress != null) {
            try {
                Map<String, Object> ipGeoMap = this.database.lookup(ipAddress);
                if (ipGeoMap != null) {
                     Stream.of(geoFieldSet).map(field -> new AbstractMap.SimpleEntry<>(nameFunction.apply(field), field.getFunction().apply(database, ipGeoMap))).
                        filter(entry -> Objects.nonNull(entry.getValue())).
                        forEach(entry -> enrichment.enrich(geoEnrichments, entry.getKey(), entry.getValue()));
                }
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

    @Override
    public void close() throws IOException {
        this.database.close();
    }
}
