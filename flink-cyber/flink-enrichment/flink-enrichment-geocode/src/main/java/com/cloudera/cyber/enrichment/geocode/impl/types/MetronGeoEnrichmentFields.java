package com.cloudera.cyber.enrichment.geocode.impl.types;

import com.maxmind.geoip2.model.CityResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@AllArgsConstructor
@Getter
public enum MetronGeoEnrichmentFields implements GeoFields {
    LOC_ID(cityResponse -> cityResponse.getCity().getGeoNameId(), "locID", "locID"),
    CITY(GeoFields::getCity, "city", "city"),
    COUNTRY(GeoFields::getCountry, "country", "country"),
    POSTAL_CODE(cityResponse -> GeoFields.convertEmptyToNull(cityResponse.getPostal().getCode()), "postalCode", "postalCode"),
    DMA_CODE(cityResponse -> cityResponse.getLocation().getMetroCode(), "dmaCode", "dmaCode"),
    LATITUDE(GeoFields::getLatitude, "latitude", "latitudes"),
    LONGITUDE(GeoFields::getLongitude, "longitude", "longitudes"),
    LOCATION_POINT(MetronGeoEnrichmentFields::getLocationPoint, "location_point", "location_point");

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

    private static Object getLocationPoint(CityResponse cityResponse) {
        Double latitudeRaw = cityResponse.getLocation().getLatitude();
        Double longitudeRaw = cityResponse.getLocation().getLongitude();
        if (latitudeRaw == null || longitudeRaw == null) {
            return null;
        } else {
            return GeoFields.convertNullToEmptyString(latitudeRaw) + "," + GeoFields.convertNullToEmptyString(longitudeRaw);
        }
    }

    static Map<String, MetronGeoEnrichmentFields> singularNameMap;

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
