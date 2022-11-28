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

package com.cloudera.cyber.enrichment.geocode.impl.types;

import com.maxmind.geoip2.model.CityResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Function;

/**
 * All geocode enrichments that could be returned for an IP.
 */
@AllArgsConstructor
@Getter
public enum GeoEnrichmentFields implements GeoFields {
    CITY(GeoFields::getCity, "city", "cities"),
    COUNTRY(GeoFields::getCountry, "country", "countries"),
    STATE(GeoFields::getState, "state", "states"),
    LATITUDE(GeoFields::getLatitude, "latitude", "latitudes"),
    LONGITUDE(GeoFields::getLongitude, "longitude", "longitudes");

    /**
     * Obtains the enrichment from the maxmind city response.
     */
    private final Function<CityResponse, Object> function;

    /**
     * Enrichment name when used on a single IP.
     */
    private final String singularName;

    /**
     * Enrichment name when used on a list of IPs.
     */
    private final String pluralName;
}
