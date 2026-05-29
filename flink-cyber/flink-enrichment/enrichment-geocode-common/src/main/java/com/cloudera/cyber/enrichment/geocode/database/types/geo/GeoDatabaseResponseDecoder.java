package com.cloudera.cyber.enrichment.geocode.database.types.geo;

import java.util.Map;

public interface GeoDatabaseResponseDecoder {
    String getCity(Map<String, Object> data);
    String getCountry(Map<String, Object> data);
    String getState(Map<String, Object> data);
    Double getLatitude(Map<String, Object> data);
    Double getLongitude(Map<String, Object> data);
    Long getLocationId(Map<String, Object> data);
    String getPostalCode(Map<String, Object> data);
    Integer getDmaCode(Map<String, Object> data);
}
