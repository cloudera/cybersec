package com.cloudera.cyber.enrichment.geocode.database.types.geo;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MaxmindGeoResponseDecoderTest extends GeoDatabaseResponseDecoderTest {

    public static final String EXPECTED_POSTAL_CODE = "OX1";
    public static final long EXPECTED_LOCATION_ID = 2655045L;
    private final Map<String, Object> response;

    private static final String EXPECTED_COUNTRY = "GB";
    private static final String EXPECTED_CITY = "Boxford";
    private static final String EXPECTED_STATE = "West Berkshire";
    public static final double EXPECTED_LATITUDE = 51.75D;
    public static final double EXPECTED_LONGITUDE = -1.25D;
    public static final int EXPECTED_DMA_CODE = 478;

    public MaxmindGeoResponseDecoderTest() {
        super(new MaxmindGeoResponseDecoder());

        Map<String, Object> countryNames = new HashMap<>();
        countryNames.put(MaxmindGeoResponseDecoder.ENGLISH_NAME_KEY, "United Kingdom");
        countryNames.put("fr", "Royaume-Uni");

        Map<String, Object> country = new HashMap<>();
        country.put(MaxmindGeoResponseDecoder.COUNTRY_KEY, countryNames);
        country.put(MaxmindGeoResponseDecoder.ISO_CODE_KEY, EXPECTED_COUNTRY);

        country.put(MaxmindGeoResponseDecoder.GEONAME_ID_KEY,2635167L);

        Map<String, Object> cityNames = createEnglishNameMap(EXPECTED_CITY);

        Map<String, Object> city = new HashMap<>();
        city.put(MaxmindGeoResponseDecoder.GEONAME_ID_KEY, EXPECTED_LOCATION_ID);
        city.put(MaxmindGeoResponseDecoder.NAMES_KEY, cityNames);


        Map<String, Object> location = new HashMap<>();
        location.put(MaxmindGeoResponseDecoder.LATITUDE_KEY, EXPECTED_LATITUDE);
        location.put(MaxmindGeoResponseDecoder.LONGITUDE_KEY, EXPECTED_LONGITUDE);
        location.put(MaxmindGeoResponseDecoder.METRO_CODE_KEY, EXPECTED_DMA_CODE);

        Map<String, Object> postal = new HashMap<>();
        postal.put(MaxmindGeoResponseDecoder.CODE_KEY, EXPECTED_POSTAL_CODE);
        this.response = new HashMap<>();
        response.put(MaxmindGeoResponseDecoder.COUNTRY_KEY, country);
        response.put(MaxmindGeoResponseDecoder.CITY_KEY, city);
        response.put(MaxmindGeoResponseDecoder.LOCATION_KEY, location);
        response.put(MaxmindGeoResponseDecoder.POSTAL_KEY, postal);

        List<Object> subdivisions = new ArrayList<>();
        subdivisions.add(createSubdivisionMap("ENG", 6269131L, "England"));
        subdivisions.add(createSubdivisionMap("WBK", 3333217L, EXPECTED_STATE));

        response.put(MaxmindGeoResponseDecoder.SUBDIVISIONS_KEY, subdivisions);
    }

    private Map<String, Object> createSubdivisionMap(String isoCode, Long id, String englishName) {
        Map<String, Object> subdivision = new HashMap<>();
        subdivision.put(MaxmindGeoResponseDecoder.ISO_CODE_KEY, isoCode);
        subdivision.put(MaxmindGeoResponseDecoder.GEONAME_ID_KEY, id);
        subdivision.put(MaxmindGeoResponseDecoder.NAMES_KEY, createEnglishNameMap(englishName));
        return subdivision;
    }

    private Map<String, Object> createEnglishNameMap(String englishName) {
        return createLanguageNameMap(MaxmindGeoResponseDecoder.ENGLISH_NAME_KEY, englishName);
    }

    private Map<String, Object> createLanguageNameMap(String language, String name) {
        Map<String, Object> names = new HashMap<>();
        names.put(language,  name);

        return names;
    }

    @Test
    public void testValidCity() {
        assertEquals(EXPECTED_CITY, decoder.getCity(response));
    }

    @Test
    public void testCityNamesNotMap() {
        Map<String, Object> namesNotMap = new HashMap<>();
        namesNotMap.put(MaxmindGeoResponseDecoder.NAMES_KEY, "unexpected string");

        Map<String, Object> response = new HashMap<>();
        response.put(MaxmindGeoResponseDecoder.CITY_KEY, namesNotMap);
        assertNull(decoder.getCity(response));
    }

    @Test
    public void testCityNoEnglishName() {
        Map<String, Object> noEnglishName = new HashMap<>();
        noEnglishName.put(MaxmindGeoResponseDecoder.NAMES_KEY, createLanguageNameMap("fr", "paris"));

        Map<String, Object> response = new HashMap<>();
        response.put(MaxmindGeoResponseDecoder.CITY_KEY, noEnglishName);
        assertNull(decoder.getCity(response));
    }

    @Test
    public void testValidPostalCode() {
        assertEquals(EXPECTED_POSTAL_CODE, decoder.getPostalCode(response));
    }

    @Test
    public void testNonStringPostalCode() {
        Map<String, Object> postal = new HashMap<>();
        postal.put(MaxmindGeoResponseDecoder.CODE_KEY, 12345);

        Map<String, Object> response = new HashMap<>();
        response.put(MaxmindGeoResponseDecoder.POSTAL_KEY, postal);

        assertNull(decoder.getPostalCode(response));
    }

    @Test
    public void testValidLocationId() {
        assertEquals(EXPECTED_LOCATION_ID, decoder.getLocationId(response));
    }

    @Test
    public void testLocationIdNotInteger() {
        Map<String, Object> city = new HashMap<>();
        city.put(MaxmindGeoResponseDecoder.GEONAME_ID_KEY, "Not an integer");

        Map<String, Object> response = new HashMap<>();
        response.put(MaxmindGeoResponseDecoder.CITY_KEY, city);
        assertNull(decoder.getLocationId(response));
    }

    @Test
    public void testValidDmaCode() {
        assertEquals(EXPECTED_DMA_CODE, decoder.getDmaCode(response));
    }

    @Test
    public void testLocationWithNoDmaCode() {
        Map<String, Object> location = new HashMap<>();
        location.put(MaxmindGeoResponseDecoder.LATITUDE_KEY, EXPECTED_LATITUDE);
        location.put(MaxmindGeoResponseDecoder.LONGITUDE_KEY, EXPECTED_LONGITUDE);

        Map<String, Object> locationWithNoDMA = new HashMap<>();
        locationWithNoDMA.put(MaxmindGeoResponseDecoder.LOCATION_KEY, location);

        assertNull(decoder.getDmaCode(locationWithNoDMA));
    }

    @Test
    public void testValidState() {
       assertEquals(EXPECTED_STATE, decoder.getState(response));
    }

    @Test
    public void testEmptyStateMap() {
        Map<String, Object> emptySubdivisionsResponse = new HashMap<>();
        emptySubdivisionsResponse.put(MaxmindGeoResponseDecoder.SUBDIVISIONS_KEY, Lists.emptyList());
        assertNull(decoder.getState(emptySubdivisionsResponse));
    }

    @Test
    public void testStatesNotMaps() {
        Map<String, Object> emptySubdivisionsResponse = new HashMap<>();
        // create a list of strings instead of maps
        emptySubdivisionsResponse.put(MaxmindGeoResponseDecoder.SUBDIVISIONS_KEY, Arrays.asList("A", "B", "C"));
        assertNull(decoder.getState(emptySubdivisionsResponse));
    }

    @Test
    public void testValidCountry() {
        assertEquals(EXPECTED_COUNTRY, decoder.getCountry(response));
    }

    @Test
    public void testNonStringCountryCode() {
        Map<String, Object> country = new HashMap<>();
        country.put(MaxmindGeoResponseDecoder.ISO_CODE_KEY, 555L);

        Map<String, Object> response = new HashMap<>();
        response.put(MaxmindGeoResponseDecoder.COUNTRY_KEY, country);
        assertNull(decoder.getCountry(response));
    }

    @Test
    public void testValidLatitude() {
        assertEquals(EXPECTED_LATITUDE, decoder.getLatitude(response));
    }

    @Test
    public void testNonDoubleLatitude() {
        Map<String, Object> location = new HashMap<>();
        location.put(MaxmindGeoResponseDecoder.LATITUDE_KEY, "string latitude");

        Map<String, Object> response = new HashMap<>();
        response.put(MaxmindGeoResponseDecoder.LOCATION_KEY, location);

        assertNull(decoder.getLatitude(response));
    }

    @Test
    public void testValidLongitude() {
       assertEquals(EXPECTED_LONGITUDE, decoder.getLongitude(response));
    }

}
