package com.cloudera.cyber.enrichment.geocode.database.types.geo;

import java.util.Map;

public class IpInfoGeoResponseDecoder implements GeoDatabaseResponseDecoder {

    public static final String LOCATION_ID_KEY = "geoname_id";
    public static final String CITY_KEY = "city";
    public static final String STATE_KEY = "region";
    public static final String POSTAL_CODE_KEY = "postal_code";
    public static final String COUNTRY_KEY = "country_code";
    public static final String LATITUDE_KEY = "latitude";
    public static final String LONGITUDE_KEY = "longitude";

    public String getState(Map<String, Object> responseData) {
        return getStringValue(responseData, STATE_KEY);
    }

    public String getCity(Map<String, Object> responseData) {
        return getStringValue(responseData, CITY_KEY);
    }

    public Long getLocationId(Map<String, Object> responseData) {
        if (responseData != null && responseData.get(LOCATION_ID_KEY) instanceof String stringValue && !stringValue.isEmpty()) {
            try {
                return Long.valueOf(stringValue);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public String getPostalCode(Map<String, Object> responseData) {
        return getStringValue(responseData, POSTAL_CODE_KEY);
    }

    @Override
    public Integer getDmaCode(Map<String, Object> data) {
        return null;
    }

    private String getStringValue(Map<String, Object> responseData, String key) {
        if (responseData != null && responseData.get(key) instanceof String stringValue) {
            return ValueConversions.convertEmptyToNull(stringValue);
        }
        return null;
    }

    public String getCountry(Map<String, Object> responseData) {
        return getStringValue(responseData, COUNTRY_KEY);
    }

    public Double getLatitude(Map<String, Object> responseData) {
        return getLocationCoordinate(responseData, LATITUDE_KEY);
    }

    public Double getLongitude(Map<String, Object> responseData) {
        return getLocationCoordinate(responseData, LONGITUDE_KEY);
    }

    private Double getLocationCoordinate(Map<String, Object> responseData, String coordinateName) {
        if (responseData != null && responseData.get(coordinateName) instanceof Double coord) {
            return coord;
        }
        return null;
    }
}
