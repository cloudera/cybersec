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

import java.util.Map;
import java.util.function.BiFunction;

/**
 * All geocode enrichments that could be returned for an IP.
 */
@AllArgsConstructor
@Getter
public enum GeoEnrichmentFields implements GeoFields {
    CITY(GeoDatabase::getCity, "city", "cities"),
    COUNTRY(GeoDatabase::getCountry, "country", "countries"),
    STATE(GeoDatabase::getState, "state", "states"),
    LATITUDE(GeoDatabase::getLatitude, "latitude", "latitudes"),
    LONGITUDE(GeoDatabase::getLongitude, "longitude", "longitudes");

    /**
     * Obtains the enrichment from ip to geolocation call.
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
}
