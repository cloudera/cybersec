package com.cloudera.cyber.enrichment.geocode;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.DataQualityMessageLevel;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.TestUtils;
import org.apache.flink.configuration.Configuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.*;

@Ignore
public class IpGeoMapTest {

    private static final String SINGLE_IP_FIELD_NAME = "ip_dst_addr";
    private static final String LIST_IPS_FIELD_NAME = "dns.answers";
    private static final List<String> ENRICH_FIELD_NAMES = Arrays.asList(SINGLE_IP_FIELD_NAME, LIST_IPS_FIELD_NAME);
    private IpGeoMap geoMap;

    @Before
    public void createGeoMap() {
        geoMap = new IpGeoMap(IpGeoTestData.GEOCODE_DATABASE_PATH, ENRICH_FIELD_NAMES, null);
        geoMap.open(new Configuration());
    }

    @Test
    public void testSingleIpAddress() {
        Map<String, String> inputFields = new HashMap<String, String>() {{
            put(SINGLE_IP_FIELD_NAME, IpGeoTestData.COUNTRY_ONLY_IPv6);
        }};
        Message output = testGeoMap(inputFields);
        assertNoErrorsOrInfos(output);
    }


    @Test
    public void testListIpAddressEmpty() {
        Map<String, String> initialExtensions = new HashMap<>();
        initialExtensions.put(LIST_IPS_FIELD_NAME, IpGeoTestData.LOCAL_IP);
        Message input = TestUtils.createMessage(initialExtensions);
        Message output = geoMap.map(input);
        Assert.assertEquals(1, output.getExtensions().size());
        assertNoErrorsOrInfos(output);
    }

    @Test
    public void testFieldNotSet() {
        Message input = TestUtils.createMessage();
        Message output = geoMap.map(input);
        Assert.assertNull(output.getExtensions());
        assertNoErrorsOrInfos(output);
    }

    @Test(expected = IllegalStateException.class)
    public void testThrowsCityDatabaseDoesNotExist() {
        String doesntExistPath = "./src/test/resources/geolite/doesntexist";
        File databaseFile = new File(doesntExistPath);
        Assert.assertFalse(databaseFile.exists());
        IpGeoMap map = new IpGeoMap(doesntExistPath, ENRICH_FIELD_NAMES, null);
        map.open(new Configuration());
    }

    @Test(expected = IllegalStateException.class)
    public void testThrowsCityDatabaseEmptyFile() {
        String emptyFilePath = "./src/test/resources/geolite/invalid_maxmind_db";
        File databaseFile = new File(emptyFilePath);
        Assert.assertTrue(databaseFile.exists());
        Assert.assertTrue(databaseFile.length() > 0);
        IpGeoMap map = new IpGeoMap(emptyFilePath, ENRICH_FIELD_NAMES, null);
        map.open(new Configuration());
    }

    @Test(expected = IllegalStateException.class)
    public void testThrowsBadFilesystem() {
        String badFilesystemPath = "bad:/src/test/resources/geolite/invalid_maxmind_db";
        IpGeoMap map = new IpGeoMap(badFilesystemPath, ENRICH_FIELD_NAMES, null);
        map.open(new Configuration());
    }

    private Message testGeoMap(Map<String, String> inputFields) {
        Message input = TestUtils.createMessage(inputFields);
        Map<String, String> expected = new HashMap<>(input.getExtensions());
        inputFields.forEach((field, value) -> IpGeoTestData.getExpectedEnrichmentValues(expected, field, value));
        Message output = geoMap.map(input);
        Assert.assertEquals(expected, output.getExtensions());

        return output;
    }

    private void assertNoErrorsOrInfos(Message output) {
        List<DataQualityMessage> dataQualityMessages = output.getDataQualityMessages();
        Assert.assertTrue(dataQualityMessages == null || dataQualityMessages.isEmpty());
    }

    private void verifyInfoMessage(Message output, String infoMessage) {
        Collection<DataQualityMessage> dqMessages = output.getDataQualityMessages();
        Assert.assertEquals(1, dqMessages.size());
        DataQualityMessage firstMessage = output.getDataQualityMessages().get(0);
        Assert.assertEquals(DataQualityMessageLevel.INFO, firstMessage.getLevel());
        Assert.assertEquals(IpGeoMapTest.LIST_IPS_FIELD_NAME, firstMessage.getField());
        Assert.assertEquals(IpGeoMap.GEOCODE_FEATURE, firstMessage.getFeature());
        Assert.assertEquals(infoMessage, firstMessage.getMessage());
    }

}
