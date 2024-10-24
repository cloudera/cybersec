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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.TestUtils;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.flink.configuration.Configuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class IpAsnMapTest {
    private static final String IP_FIELD_NAME = "ip_dst_addr";
    private static final List<String> ENRICH_FIELD_NAMES = Collections.singletonList(IP_FIELD_NAME);
    private IpAsnMap asnMap;

    @Before
    public void createAsnMap() {
         asnMap = new IpAsnMap(IpAsnTestData.ASN_DATABASE_PATH, ENRICH_FIELD_NAMES, null);
         asnMap.open(new Configuration());
    }

    @Test
    public void testNoAsnIpFields() {
        IpAsnMap emptyFields = new IpAsnMap(IpAsnTestData.ASN_DATABASE_PATH, Collections.emptyList(), null);
        emptyFields.open(new Configuration());
        Map<String, String> inputFields = new HashMap<>();
        inputFields.put(IP_FIELD_NAME, IpAsnTestData.IP_WITH_NUMBER_AND_ORG);
        Message result = emptyFields.map(TestUtils.createMessage(inputFields));
        Assert.assertEquals(inputFields, result.getExtensions());
        assertNoErrorsOrInfos(result);
    }

    @Test
    public void testFieldNotDefined() {
        Message emptyMessage = asnMap.map(TestUtils.createMessage(Collections.emptyMap()));
        Assert.assertEquals(Collections.emptyMap(), emptyMessage.getExtensions());
        assertNoErrorsOrInfos(emptyMessage);
    }

    @Test
    public void testFieldsNull() {
        Message emptyMessage = asnMap.map(TestUtils.createMessage());
        Assert.assertNull(emptyMessage.getExtensions());
        assertNoErrorsOrInfos(emptyMessage);
    }


    @Test
    public void testIpAddress() {
        Map<String, String> inputFields = new HashMap<String, String>() {{
            put(IP_FIELD_NAME, IpAsnTestData.IP_WITH_NUMBER_AND_ORG);
        }};
        Message output = testAsnMap(inputFields);
        assertNoErrorsOrInfos(output);
    }


    @Test
    public void testFieldNotSet() {
        Message input = TestUtils.createMessage();
        Message output = asnMap.map(input);
        Assert.assertNull(output.getExtensions());
        assertNoErrorsOrInfos(output);
    }

    @Test
    public void testThrowsAsnDatabaseDoesNotExist() {
        String doesntExistPath = "./src/test/resources/geolite/doesntexist";
        File databaseFile = new File(doesntExistPath);
        Assert.assertFalse(databaseFile.exists());
        IpAsnMap map = new IpAsnMap(doesntExistPath, ENRICH_FIELD_NAMES, null);
        assertThatThrownBy(() -> map.open(new Configuration()))
                .isInstanceOfAny(IllegalStateException.class)
                .hasMessage("Could not read asn database %s", doesntExistPath);
    }

    @Test
    public void testThrowsAsnDatabaseEmptyFile() {
        String emptyFilePath = "./src/test/resources/geolite/invalid_maxmind_db";
        File databaseFile = new File(emptyFilePath);
        Assert.assertTrue(databaseFile.exists());
        Assert.assertTrue(databaseFile.length() > 0);
        IpAsnMap map = new IpAsnMap(emptyFilePath, ENRICH_FIELD_NAMES, null);
        assertThatThrownBy(() ->map.open(new Configuration())).isInstanceOfAny(IllegalStateException.class)
                .hasMessage("Could not read asn database %s", emptyFilePath);
    }

    @Test
    public void testThrowsBadFilesystem() {
        String badFilesystemPath = "bad:/src/test/resources/geolite/invalid_maxmind_db";
        IpAsnMap map = new IpAsnMap(badFilesystemPath, ENRICH_FIELD_NAMES, null);
        assertThatThrownBy(() ->map.open(new Configuration())).isInstanceOfAny(IllegalStateException.class)
                .hasMessage("Could not read asn database %s", badFilesystemPath);
    }

    private Message testAsnMap(Map<String, String> inputFields) {
        Message input = TestUtils.createMessage(inputFields);
        Map<String, String> expected = new HashMap<>(input.getExtensions());
        inputFields.forEach((field, value) -> expected.putAll(IpAsnTestData.getExpectedValues(field, value)));
        Message output = asnMap.map(input);
        Assert.assertEquals(expected, output.getExtensions());

        return output;
    }

    private void assertNoErrorsOrInfos(Message output) {
        List<DataQualityMessage> dataQualityMessages = output.getDataQualityMessages();
        Assert.assertTrue(dataQualityMessages == null || dataQualityMessages.isEmpty());
    }

}
