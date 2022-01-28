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
