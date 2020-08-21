package com.cloudera.cyber.enrichment.geocode;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.TestUtils;
import com.cloudera.cyber.data.quality.DataQualityMessage;
import com.cloudera.cyber.data.quality.MessageLevel;
import org.apache.flink.configuration.Configuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.*;

public class IpGeoMapTest {

    private static final String SINGLE_IP_FIELD_NAME = "ip_dst_addr";
    private static final String LIST_IPS_FIELD_NAME = "dns.answers";
    private static final List<String> ENRICH_FIELD_NAMES = Arrays.asList(SINGLE_IP_FIELD_NAME, LIST_IPS_FIELD_NAME);
    private IpGeoMap geoMap;

    @Before
    public void createGeoMap() throws Exception {
        geoMap = new IpGeoMap(IpGeoTestData.GEOCODE_DATABASE_PATH, ENRICH_FIELD_NAMES, null);
        geoMap.open(new Configuration());
    }

    @Test
    public void testSingleIpAddress() {
        Map<String, Object> inputFields = new HashMap<String, Object>() {{
            put(SINGLE_IP_FIELD_NAME, IpGeoTestData.COUNTRY_ONLY_IPv6);
        }};
        Message output = testGeoMap(inputFields);
        assertNoErrorsOrInfos(output);
    }

    @Test
    public void testListIpAddress() {
        Map<String, Object> inputFields = new HashMap<String, Object>() {{
            put(LIST_IPS_FIELD_NAME, Arrays.asList(IpGeoTestData.LOCAL_IP, IpGeoTestData.COUNTRY_ONLY_IPv6, IpGeoTestData.COUNTRY_ONLY_IPv6, IpGeoTestData.UNKNOWN_HOST_IP, IpGeoTestData.ALL_FIELDS_IPv4));
        }};
        Message output = testGeoMap(inputFields);
        verifyInfoMessage(output, LIST_IPS_FIELD_NAME, String.format(IpGeoMap.FIELD_VALUE_IS_NOT_A_VALID_IP_ADDRESS, IpGeoTestData.UNKNOWN_HOST_IP));
    }

    @Test
    public void testListIpAddressEmpty() {
        Message input = TestUtils.createMessage();
        input.getExtensions().put(LIST_IPS_FIELD_NAME, Collections.singletonList(IpGeoTestData.LOCAL_IP));
        Message output = geoMap.map(input);
        Assert.assertEquals(1, output.getExtensions().size());
        assertNoErrorsOrInfos(output);
    }

    @Test
    public void testFieldNotSet() {
        Message input = TestUtils.createMessage();
        Message output = geoMap.map(input);
        Assert.assertTrue(output.getExtensions().isEmpty());
        assertNoErrorsOrInfos(output);
    }

    @Test
    public void testIpListElementIsNotString() {
        Map<String, Object> fields = new HashMap<>();
        Integer wrongTypeInList = 400;
        fields.put(LIST_IPS_FIELD_NAME, Arrays.asList(IpGeoTestData.ALL_FIELDS_IPv4, wrongTypeInList));
        Message output = testGeoMap(fields);
        verifyInfoMessage(output, LIST_IPS_FIELD_NAME, String.format(IpGeoMap.FIELD_VALUE_IS_NOT_A_STRING, wrongTypeInList.toString()));
    }

    @Test(expected = IllegalStateException.class)
    public void testThrowsCityDatabaseDoesNotExist() throws Exception {
        String doesntExistPath = "./src/test/resources/geolite/doesntexist";
        File databaseFile = new File(doesntExistPath);
        Assert.assertFalse(databaseFile.exists());
        IpGeoMap map = new IpGeoMap(doesntExistPath, ENRICH_FIELD_NAMES, null);
        map.open(new Configuration());
    }

    @Test(expected = IllegalStateException.class)
    public void testThrowsCityDatabaseEmptyFile() throws Exception {
        String emptyFilePath = "./src/test/resources/geolite/invalid_maxmind_db";
        File databaseFile = new File(emptyFilePath);
        Assert.assertTrue(databaseFile.exists());
        Assert.assertTrue(databaseFile.length() > 0);
        IpGeoMap map = new IpGeoMap("./src/test/resources/geolite/invalid_maxmind_db", ENRICH_FIELD_NAMES, null);
        map.open(new Configuration());
    }

    private Message testGeoMap(Map<String, Object> inputFields) {
        Message input = TestUtils.createMessage();
        input.getExtensions().putAll(inputFields);
        Map<String, Object> expected = new HashMap<>(input.getExtensions());
        inputFields.forEach((field, value) -> IpGeoTestData.getExpectedEnrichmentValues(expected, field, value));
        Message output = geoMap.map(input);
        Assert.assertEquals(expected, output.getExtensions());

        return output;
    }

    private void assertNoErrorsOrInfos(Message output) {
        Assert.assertNull(output.getDataQualityMessages());
    }

    private void verifyInfoMessage(Message output, String fieldName, String infoMessage) {
        Collection<DataQualityMessage> dqMessages = output.getDataQualityMessages();
        Assert.assertEquals(1, dqMessages.size());
        DataQualityMessage firstMessage = output.getDataQualityMessages().get(0);
        Assert.assertEquals(MessageLevel.INFO, firstMessage.getLevel());
        Assert.assertEquals(fieldName, firstMessage.getField());
        Assert.assertEquals(IpGeoMap.GEOCODE_FEATURE, firstMessage.getFeature());
        Assert.assertEquals(infoMessage, firstMessage.getMessageText());
    }

}
