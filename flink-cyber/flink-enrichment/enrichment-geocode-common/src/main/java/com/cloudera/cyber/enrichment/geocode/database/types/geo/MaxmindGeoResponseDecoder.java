package com.cloudera.cyber.enrichment.geocode.database.types.geo;

import com.cloudera.cyber.enrichment.geocode.database.types.ValueConversions;

import java.util.List;
import java.util.Map;

public class MaxmindGeoResponseDecoder implements GeoDatabaseResponseDecoder {

    public static final String SUBDIVISIONS_KEY = "subdivisions";
    public static final String CITY_KEY = "city";
    public static final String GEONAME_ID_KEY = "geoname_id";
    public static final String POSTAL_KEY = "postal";
    public static final String CODE_KEY = "code";
    public static final String LOCATION_KEY = "location";
    public static final String METRO_CODE_KEY = "metro_code";
    public static final String NAMES_KEY = "names";
    public static final String ENGLISH_NAME_KEY = "en";
    public static final String COUNTRY_KEY = "country";
    public static final String ISO_CODE_KEY = "iso_code";
    public static final String LATITUDE_KEY = "latitude";
    public static final String LONGITUDE_KEY = "longitude";

    @Override
    @SuppressWarnings("unchecked")
    public String getState(Map<String, Object> responseData) {
        if (responseData != null && responseData.get(SUBDIVISIONS_KEY) instanceof List<?> subdivisions) {
            if (!subdivisions.isEmpty() && subdivisions.get(subdivisions.size() - 1) instanceof Map<?,?> mostSpecificSubdivision) {
                return getEnglishName((Map<String, Object>)mostSpecificSubdivision);
            }
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getCity(Map<String, Object> responseData) {
        if (responseData != null && responseData.get(CITY_KEY) instanceof Map<?, ?> city) {
            return getEnglishName((Map<String, Object>)city);
        }
        return null;
    }

    @Override
    public Long getLocationId(Map<String, Object> responseData) {
        if (responseData != null && responseData.get(CITY_KEY) instanceof Map<?, ?> city) {
            if (city.get(GEONAME_ID_KEY) instanceof Long locationId) {
                return locationId;
            }
        }
        return null;
    }

    @Override
    public String getPostalCode(Map<String, Object> responseData) {
        if (responseData != null && responseData.get(POSTAL_KEY) instanceof Map<?,?> postal) {
            if (postal.get(CODE_KEY) instanceof String postalCode) {
                return ValueConversions.convertEmptyToNull(postalCode);
            }
        }
        return null;
    }

    @Override
    public Integer getDmaCode(Map<String, Object> data) {
        if(data != null && data.get(LOCATION_KEY) instanceof Map<?, ?> location) {
            if (location.get(METRO_CODE_KEY) instanceof Integer metroCode) {
                return metroCode;
            }
        }
        return null;
    }

    private String getEnglishName(Map<String, Object> parentMap) {
        if (parentMap.get(NAMES_KEY) instanceof Map<?,?> names) {
            if (names.get(ENGLISH_NAME_KEY) instanceof String englishState) {
                return ValueConversions.convertEmptyToNull(englishState);
            }
        }
        return null;
    }

    @Override
    public String getCountry(Map<String, Object> data) {
        if (data != null && data.get(COUNTRY_KEY) instanceof Map<?,?> countryMap) {
            if (countryMap.get(ISO_CODE_KEY) instanceof String isoCode) {
                return ValueConversions.convertEmptyToNull(isoCode);
            }
        }
        return null;
    }

    @Override
    public Double getLatitude(Map<String, Object> data) {
        return getLocationCoordinate(data, LATITUDE_KEY);
    }

    @Override
    public Double getLongitude(Map<String, Object> data) {
        return getLocationCoordinate(data, LONGITUDE_KEY);
    }

    private Double getLocationCoordinate(Map<String, Object> data, String coordinateName) {
        if (data != null && data.get(LOCATION_KEY) instanceof Map<?, ?> location) {
            if (location.get(coordinateName) instanceof Double coordinateValue) {
                return coordinateValue;
            }
        }
        return null;
    }

}
