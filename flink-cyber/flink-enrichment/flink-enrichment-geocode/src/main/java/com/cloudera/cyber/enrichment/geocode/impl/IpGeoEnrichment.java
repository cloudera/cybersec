package com.cloudera.cyber.enrichment.geocode.impl;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.DataQualityMessageLevel;
import com.cloudera.cyber.enrichment.Enrichment;
import com.cloudera.cyber.enrichment.SingleValueEnrichment;
import com.maxmind.geoip2.DatabaseProvider;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Location;
import com.maxmind.geoip2.record.Subdivision;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.net.InetAddress;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Looks up the locations of IPv4 or IPv6 addresses in MaxMind GeoLite2 city database and returns
 * the locations for that IP.
 */
public class IpGeoEnrichment extends MaxMindBase {
    public static final String GEOCODE_FEATURE = "geo";
    public static final String GEOCODE_FAILED_MESSAGE = "Geocode failed '%s'";

    public IpGeoEnrichment(DatabaseProvider database) {
        super(database);
    }

    /**
     * All geocode enrichments that could be returned for an IP.
     */
    @AllArgsConstructor
    public enum GeoEnrichmentFields {
        CITY(IpGeoEnrichment::getCity, "city", "cities"),
        COUNTRY(IpGeoEnrichment::getCountry, "country", "countries"),
        STATE(IpGeoEnrichment::getState, "state", "states"),
        LATITUDE(IpGeoEnrichment::getLatitude, "latitude", "latitudes"),
        LONGITUDE(IpGeoEnrichment::getLongitude, "longitude", "longitudes");

        /**
         * Obtains the enrichment from the maxmind city response.
         */
        @Getter
        private final Function<CityResponse, Object> function;

        /**
         * Enrichment name when used on a single IP.
         */
        @Getter
        private final String singularName;

        /**
         * Enrichment name when used on a list of IPs.
         */
        @Getter
        private final String pluralName;
    }

    private static Object getCity(CityResponse cityResponse) {
        return convertEmptyToNull(cityResponse.getCity().getName());
    }

    private static Object getCountry(CityResponse cityResponse) {
        Country country = cityResponse.getCountry();
        String countryCode = null;
        if (country != null) {
            countryCode = convertEmptyToNull(country.getIsoCode());
        }

        return countryCode;
    }

    private static Object getState(CityResponse cityResponse) {
        Subdivision subdivision = cityResponse.getMostSpecificSubdivision();
        String stateName = null;
        if (subdivision != null ) {
            stateName = convertEmptyToNull(subdivision.getName());
        } 
        
        return stateName;
    }

    private static Object getLatitude(CityResponse cityResponse) {
        Location location = cityResponse.getLocation();
        Double latitude = null;
        if (location != null) {
            latitude = location.getLatitude();
        }
        return latitude;
    }

    private static Object getLongitude(CityResponse cityResponse) {
        Location location = cityResponse.getLocation();
        Double longitude = null;
        if (location != null) {
            longitude = location.getLongitude();
        }
        return longitude;
    }

    /**
     * Lookup the IP in the city database and return the geo location enrichment fields.
     *
     * @param ipFieldValue A valid IPv4 or IPv6 address represented in a string.
     * @param geoFieldSet Only Geocoding fields specified are returned.
     *                    For example if the IP has a city in the database but city is not included in this set, the city will not be returned.
     */
    private void lookup(Enrichment enrichment, Function<GeoEnrichmentFields, String> nameFunction, Object ipFieldValue, GeoEnrichmentFields[] geoFieldSet, Map<String, String> geoEnrichments, List<DataQualityMessage> qualityMessages) {

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

    public void lookup(String fieldName, Object ipFieldValue, GeoEnrichmentFields[] geoFieldSet, Map<String, String> geoEnrichments, List<DataQualityMessage> qualityMessages) {
        if (ipFieldValue instanceof Collection) {
            Enrichment enrichment = new SingleValueEnrichment(fieldName, GEOCODE_FEATURE);
            //noinspection unchecked
            ((Collection<Object>) ipFieldValue).forEach(ip -> lookup(enrichment, GeoEnrichmentFields::getPluralName, ip, geoFieldSet, geoEnrichments, qualityMessages));
        } else if (ipFieldValue != null) {
            lookup(new SingleValueEnrichment(fieldName, GEOCODE_FEATURE), GeoEnrichmentFields::getSingularName, ipFieldValue, geoFieldSet, geoEnrichments, qualityMessages);
        }
    }

}
