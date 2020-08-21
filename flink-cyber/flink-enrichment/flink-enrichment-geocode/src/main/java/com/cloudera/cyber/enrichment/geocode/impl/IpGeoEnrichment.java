package com.cloudera.cyber.enrichment.geocode.impl;

import avro.shaded.com.google.common.base.Preconditions;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Location;
import com.maxmind.geoip2.record.Subdivision;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * Looks up the locations of IPv4 or IPv6 addresses in MaxMind GeoLite2 city database and returns
 * the locations for that IP.
 */
@Log
public class IpGeoEnrichment  {

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
         * Enrichment naem when used on a list of IPs.
         */
        @Getter
        private final String pluralName;
    }


    /**
     * Parsed and cached Maxmind city database.
     */
    private final DatabaseReader cityDatabase;

    public IpGeoEnrichment(DatabaseReader cityDatabase) {
        Preconditions.checkNotNull(cityDatabase);
        this.cityDatabase = cityDatabase;
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

    private static String convertEmptyToNull(String str) {
        return StringUtils.isBlank(str) ? null : str;
    }

    private void extractGeoEnrichmentFieldsFromCityResponse(GeoEnrichmentFields[] geoFieldSet, CityResponse response, Map<GeoEnrichmentFields, Object> geoEnrichments) {

        for(GeoEnrichmentFields geoField : geoFieldSet) {
            Object value = geoField.getFunction().apply(response);
            if (value != null) {
                geoEnrichments.put(geoField, value);
            }
        }

    }

    /**
     * Lookup the IP in the city database and return the
     *
     * @param ipFieldValue A valid IPv4 or IPv6 address represented in a string.
     * @param geoFieldSet Only Geocoding fields specified are returned.
     *                    For example if the IP has a city in the database but city is not included in this set, the city will not be returned.
     * @return Map of specified city location fields for given ip address.
     * @throws Exception When geocoding fails or the ip is invalid.
     */
    public Map<GeoEnrichmentFields, Object> lookup(String ipFieldValue, GeoEnrichmentFields[] geoFieldSet) throws Exception {
        Map<GeoEnrichmentFields, Object> geoEnrichments = new HashMap<>();

        try {
            InetAddress ipAddress = InetAddress.getByName(ipFieldValue);
            cityDatabase.tryCity(ipAddress).ifPresent(response -> extractGeoEnrichmentFieldsFromCityResponse(geoFieldSet, response, geoEnrichments));

            return geoEnrichments;
        } catch (Exception e) {
            if (!(e instanceof UnknownHostException)) {
                log.log(Level.SEVERE,String.format("Exception while geocoding ip '%s'", ipFieldValue), e);
            }
            throw e;
        }
    }
}
