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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.cloudera.cyber.enrichment.geocode.IpCompanyTestData.INVALID_DATABASE_PATH;
import static com.cloudera.cyber.enrichment.geocode.IpCompanyTestData.IP_FIELD_NAME;
import static com.cloudera.cyber.enrichment.geocode.IpCompanyTestData.LOCAL_IP;
import static com.cloudera.cyber.enrichment.geocode.IpCompanyTestData.UNKNOWN_HOST_IP;
import static com.cloudera.cyber.enrichment.geocode.database.IpCompanyEnrichment.COMPANY_FEATURE;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for IpCompanyMap.
 */
public class IpCompanyMapTest {

    private static final List<String> ENRICH_FIELD_NAMES = Collections.singletonList(IP_FIELD_NAME);
    
    private IpCompanyMap companyMap;

    @BeforeEach
    void createCompanyMap() {
        companyMap = new IpCompanyMap(IpCompanyTestData.COMPANY_DATABASE_PATH, ENRICH_FIELD_NAMES, null);
        companyMap.open(new Configuration());
    }

    @Test
    void testLookupMultipleFields() {
        String srcAddr = "ip_src_addr";
        String dstAddr = "ip_dst_addr";
        List<String> enrichedFields = List.of(srcAddr, dstAddr);

        Map<String, String> inputFields = new HashMap<>();
        inputFields.put(srcAddr, IpCompanyTestData.IP_COMPANY_ONLY);
        inputFields.put(dstAddr, IpCompanyTestData.IP_WITH_NUMBER_AND_ORG);

        testSuccessfulMessageMap(inputFields, enrichedFields);
    }

    @Test
    void testLookupNotAnIp() {
        // look up a string that is not an IP address
        Map<String, String> inputFields = new HashMap<>();
        inputFields.put(IP_FIELD_NAME, UNKNOWN_HOST_IP);

        Message result = companyMap.map(TestUtils.createMessage(inputFields));
        // no extension added
        Assertions.assertEquals(inputFields, result.getExtensions());
        // verify data quality message
        Assertions.assertEquals(Collections.singletonList(new DataQualityMessage(DataQualityMessageLevel.INFO.name(), COMPANY_FEATURE, IP_FIELD_NAME, String.format("'%s' is not a valid IP address.", UNKNOWN_HOST_IP))),
                result.getDataQualityMessages());
    }

    @Test
    void testLookupLocalIp() {
        // look up a string that is not an IP address
        Map<String, String> inputFields = new HashMap<>();
        inputFields.put(IP_FIELD_NAME, LOCAL_IP);

        Message result = companyMap.map(TestUtils.createMessage(inputFields));

        // no extension added
        Assertions.assertEquals(inputFields, result.getExtensions());

        assertNoErrorsOrInfos(result);
    }

    @Test
    void testNoCompanyIpFieldsReturnsOriginalExtensions() {
        // add an IP that doesn't have any results - returns original message fields
        Map<String, String> inputFields = new HashMap<>();
        inputFields.put(IP_FIELD_NAME, IpCompanyTestData.IP_WITH_NO_INFO);

        testSuccessfulMessageMap(inputFields, ENRICH_FIELD_NAMES);
    }

    @Test
    void testFieldNotDefinedReturnsEmptyExtensions() {
        Message emptyMessage = companyMap.map(TestUtils.createMessage(Collections.emptyMap()));
        
        Assertions.assertEquals(Collections.emptyMap(), emptyMessage.getExtensions());
        assertNoErrorsOrInfos(emptyMessage);
    }

    @Test
    void testFieldsNullReturnsNullExtensions() {
        Message emptyMessage = companyMap.map(TestUtils.createMessage());
        
        Assertions.assertNull(emptyMessage.getExtensions());
        assertNoErrorsOrInfos(emptyMessage);
    }

    @Test
    void testThrowsOpenInvalidDatabase() {
        File databaseFile = new File(INVALID_DATABASE_PATH);
        Assertions.assertTrue(databaseFile.exists());
        Assertions.assertTrue(databaseFile.length() > 0);
        IpCompanyMap map = new IpCompanyMap(INVALID_DATABASE_PATH, ENRICH_FIELD_NAMES, null);
        assertThatThrownBy(() ->map.open(new Configuration())).isInstanceOfAny(IllegalStateException.class).
                hasMessage("Could not read company database %s", INVALID_DATABASE_PATH);
   }

    @Test
    void testThrowsOpenCompanyDatabaseDoesNotExist() {
        String nonExistentPath = "./src/test/resources/ipinfo/doesntexist";
        File databaseFile = new File(nonExistentPath);
        Assertions.assertFalse(databaseFile.exists());
        
        IpCompanyMap map = new IpCompanyMap(nonExistentPath, ENRICH_FIELD_NAMES, null);
        
        assertThatThrownBy(() -> map.open(new Configuration()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Could not read company database %s", nonExistentPath);
    }

    @Test
    void testThrowsBadFilesystem() {
        String badFilesystemPath = "bad:/src/test/resources/geolite/invalid_maxmind_db.mmdb";
        
        IpCompanyMap map = new IpCompanyMap(badFilesystemPath, ENRICH_FIELD_NAMES, null);
        
        assertThatThrownBy(() -> map.open(new Configuration()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Could not read company database %s", badFilesystemPath);
    }

    @Test
    void testCloseDoesNotThrow() throws Exception {
        IpCompanyMap map = new IpCompanyMap(IpCompanyTestData.COMPANY_DATABASE_PATH, ENRICH_FIELD_NAMES, null);
        map.open(new Configuration());
        
        // Close should not throw
        map.close();
    }

    @Test
    void testCloseWithNullEnrichmentDoesNotThrow() throws Exception {
        IpCompanyMap map = new IpCompanyMap(IpCompanyTestData.COMPANY_DATABASE_PATH, ENRICH_FIELD_NAMES, null);
        // Don't open - enrichment will be null
        
        // Close should not throw even if enrichment is null
        map.close();
    }

    @Test
    void testMessageWithMultipleFieldsProcessesOnlyConfiguredFields() {
        Map<String, String> inputFields = new HashMap<>();
        inputFields.put(IP_FIELD_NAME, IpCompanyTestData.IP_WITH_NUMBER_AND_ORG);
        inputFields.put("other_field", "other_value");
        
        List<String> onlyDstAddr = Collections.singletonList(IP_FIELD_NAME);
        IpCompanyMap singleFieldMap = new IpCompanyMap(
            IpCompanyTestData.COMPANY_DATABASE_PATH, 
            onlyDstAddr, 
            null
        );
        singleFieldMap.open(new Configuration());
        
        Message output = singleFieldMap.map(TestUtils.createMessage(inputFields));
        
        // other_field should still be present (untouched)
        Assertions.assertEquals("other_value", output.getExtensions().get("other_field"));
    }

    @Test
    void testEmptyInputFieldsMapDoesNotThrow() {
        Map<String, String> inputFields = new HashMap<>();
        inputFields.put(IP_FIELD_NAME, "");

        Message result = companyMap.map(TestUtils.createMessage(inputFields));
        Assertions.assertEquals(1, result.getDataQualityMessages().size());
    }

    private void testSuccessfulMessageMap(Map<String, String> inputFields, List<String> companyEnrichedFields) {
        IpCompanyMap multipleFieldMap = new IpCompanyMap(
                IpCompanyTestData.COMPANY_DATABASE_PATH,
                companyEnrichedFields,
                null);
        multipleFieldMap.open(new Configuration());


        Message result = multipleFieldMap.map(TestUtils.createMessage(inputFields));
        Map<String, String> expectedExtensions = IpCompanyTestData.getExpectedExtension(inputFields, companyEnrichedFields);

        assertNoErrorsOrInfos(result);
        Assertions.assertEquals(expectedExtensions, result.getExtensions());
    }

    private void assertNoErrorsOrInfos(Message output) {
        List<DataQualityMessage> dataQualityMessages = output.getDataQualityMessages();
        Assertions.assertTrue(
            dataQualityMessages == null || dataQualityMessages.isEmpty(),
            "Expected no quality messages but got: " + dataQualityMessages
        );
    }
}
