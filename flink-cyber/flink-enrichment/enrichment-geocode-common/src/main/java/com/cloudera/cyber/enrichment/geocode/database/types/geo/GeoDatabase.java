package com.cloudera.cyber.enrichment.geocode.database.types.geo;

import com.cloudera.cyber.enrichment.geocode.database.MaxmindDatabase;
import com.maxmind.db.Reader;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;

import static com.cloudera.cyber.enrichment.geocode.database.types.geo.ValueConversions.convertNullToEmptyString;

public class GeoDatabase extends MaxmindDatabase {
    private final GeoDatabaseResponseDecoder responseDecoder;

    public GeoDatabase(Reader database) {
        super(database);
        this.responseDecoder = getDecoder();
    }

    public GeoDatabase(String geocodeDatabasePath) {
        super(geocodeDatabasePath);
        this.responseDecoder = getDecoder();
    }

    public Map<String, Object> lookup(InetAddress ipAddress) throws IOException {
        if (ipAddress != null) {
            //noinspection unchecked
            return database.get(ipAddress, Map.class);
        }
        return null;
    }

    private GeoDatabaseResponseDecoder getDecoder() {
        return getDatabaseVendor().getGeoDecoder();
    }

    public  String getCity(Map<String, Object> data) {
        return this.responseDecoder.getCity(data);
    }

    public  String getCountry(Map<String, Object> data) {
        return this.responseDecoder.getCountry(data);
    }
    public  String getState(Map<String, Object> data) {
        return this.responseDecoder.getState(data);
    }

    public  Double getLatitude(Map<String, Object> data) {
        return this.responseDecoder.getLatitude(data);
    }

    public  Double getLongitude(Map<String, Object> data) {
        return this.responseDecoder.getLongitude(data);
    }

    public  Long getLocationId(Map<String, Object> data) {
        return this.responseDecoder.getLocationId(data);
    }

    public  String getPostalCode(Map<String, Object> data) {
        return this.responseDecoder.getPostalCode(data);
    }

    public Integer getDmaCode(Map<String, Object> data) {
        return this.responseDecoder.getDmaCode(data);
    }

    public Object getLocationPoint(Map<String, Object> response) {
        Double latitudeRaw = this.getLatitude(response);
        Double longitudeRaw = this.getLongitude(response);
        if (latitudeRaw == null || longitudeRaw == null) {
            return null;
        } else {
            return convertNullToEmptyString(latitudeRaw) + "," + convertNullToEmptyString(longitudeRaw);
        }
    }
}
