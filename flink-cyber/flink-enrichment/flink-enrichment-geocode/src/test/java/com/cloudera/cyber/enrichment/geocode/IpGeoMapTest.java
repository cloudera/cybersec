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
        Message input = TestUtils.createMessage(Collections.emptyMap());
        Message output = geoMap.map(input);
        Assert.assertEquals(Collections.emptyMap(), output.getExtensions());
        assertNoErrorsOrInfos(output);
    }

    @Test
    public void testNullExtensions() {
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
}
