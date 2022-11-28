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
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Location;
import com.maxmind.geoip2.record.Subdivision;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Function;


public interface GeoFields {

    /**
     * Obtains the enrichment from the maxmind city response.
     */
    Function<CityResponse, Object> getFunction();

    /**
     * Enrichment name when used on a single IP.
     */
    String getSingularName();

    /**
     * Enrichment name when used on a list of IPs.
     */
    String getPluralName();

    static Object getCity(CityResponse cityResponse) {
        return convertEmptyToNull(cityResponse.getCity().getName());
    }

    static String convertEmptyToNull(String str) {
        return StringUtils.isBlank(str) ? null : str;
    }

    static Object getCountry(CityResponse cityResponse) {
        Country country = cityResponse.getCountry();
        String countryCode = null;
        if (country != null) {
            countryCode = convertEmptyToNull(country.getIsoCode());
        }

        return countryCode;
    }

    static Object getState(CityResponse cityResponse) {
        Subdivision subdivision = cityResponse.getMostSpecificSubdivision();
        String stateName = null;
        if (subdivision != null ) {
            stateName = convertEmptyToNull(subdivision.getName());
        }

        return stateName;
    }

    static Object getLatitude(CityResponse cityResponse) {
        Location location = cityResponse.getLocation();
        Double latitude = null;
        if (location != null) {
            latitude = location.getLatitude();
        }
        return latitude;
    }

    static Object getLongitude(CityResponse cityResponse) {
        Location location = cityResponse.getLocation();
        Double longitude = null;
        if (location != null) {
            longitude = location.getLongitude();
        }
        return longitude;
    }

    /**
     * Converts null to empty string
     * @param raw The raw object
     * @return Empty string if null, or the String value if not
     */
    static String convertNullToEmptyString(Object raw) {
        return raw == null ? "" : String.valueOf(raw);
    }
}
