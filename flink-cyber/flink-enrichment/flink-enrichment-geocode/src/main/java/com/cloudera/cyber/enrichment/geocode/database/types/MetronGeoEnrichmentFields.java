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

package com.cloudera.cyber.enrichment.geocode.database.types;

import com.cloudera.cyber.enrichment.geocode.database.types.geo.GeoDatabase;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@AllArgsConstructor
@Getter
public enum MetronGeoEnrichmentFields implements GeoFields {
    LOC_ID(GeoDatabase::getLocationId, "locID", "locID"),
    CITY(GeoDatabase::getCity, "city", "city"),
    COUNTRY(GeoDatabase::getCountry, "country", "country"),
    POSTAL_CODE(GeoDatabase::getPostalCode, "postalCode", "postalCode"),
    DMA_CODE(GeoDatabase::getDmaCode, "dmaCode", "dmaCode"),
    LATITUDE(GeoDatabase::getLatitude, "latitude", "latitudes"),
    LONGITUDE(GeoDatabase::getLongitude, "longitude", "longitudes"),
    LOCATION_POINT(GeoDatabase::getLocationPoint, "location_point", "location_point");

    /**
     * Obtains the enrichment from the maxmind city response.
     */
    private final BiFunction<GeoDatabase, Map<String, Object>, Object> function;

    /**
     * Enrichment name when used on a single IP.
     */
    private final String singularName;

    /**
     * Enrichment name when used on a list of IPs.
     */
    private final String pluralName;



    static final Map<String, MetronGeoEnrichmentFields> singularNameMap;

    static {
        singularNameMap = Arrays.stream(MetronGeoEnrichmentFields.values()).collect(
                        Collectors.collectingAndThen(
                                Collectors.toMap(MetronGeoEnrichmentFields::getSingularName, Function.identity()),
                                Collections::unmodifiableMap));
    }

    public static MetronGeoEnrichmentFields fromSingularName(String singularName) {
        return singularNameMap.get(singularName);
    }
}
