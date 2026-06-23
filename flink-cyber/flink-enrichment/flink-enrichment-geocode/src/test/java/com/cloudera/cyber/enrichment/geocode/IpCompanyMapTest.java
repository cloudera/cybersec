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
import com.cloudera.cyber.Message;
import com.cloudera.cyber.TestUtils;
import org.apache.flink.configuration.Configuration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for IpCompanyMap.
 */
public class IpCompanyMapTest {

    private static final String IP_FIELD_NAME = "ip_dst_addr";
    private static final List<String> ENRICH_FIELD_NAMES = Collections.singletonList(IP_FIELD_NAME);
    
    private IpCompanyMap companyMap;

    @BeforeEach
    void createCompanyMap() {
        companyMap = new IpCompanyMap(IpCompanyTestData.COMPANY_DATABASE_PATH, ENRICH_FIELD_NAMES, null);
        companyMap.open(new Configuration());
    }

    @Test
    void testNoCompanyIpFieldsReturnsOriginalExtensions() {
        IpCompanyMap emptyFields = new IpCompanyMap(
            IpCompanyTestData.COMPANY_DATABASE_PATH, 
            Collections.emptyList(), 
            null
        );
        emptyFields.open(new Configuration());
        
        Map<String, String> inputFields = new HashMap<>();
        inputFields.put(IP_FIELD_NAME, IpCompanyTestData.CLOUDFLARE_IP);
        Message result = emptyFields.map(TestUtils.createMessage(inputFields));
        
        // With empty field names, original fields should be unchanged
        Assertions.assertEquals(inputFields, result.getExtensions());
        assertNoErrorsOrInfos(result);
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
    void testFieldNotSetReturnsNullExtensions() {
        Message input = TestUtils.createMessage();
        Message output = companyMap.map(input);
        
        Assertions.assertNull(output.getExtensions());
        assertNoErrorsOrInfos(output);
    }

    @Test
    void testIpAddressWithInvalidDatabaseAddsErrorMessage() {
        Map<String, String> inputFields = new HashMap<>();
        inputFields.put(IP_FIELD_NAME, IpCompanyTestData.CLOUDFLARE_IP);
        
        Message output = companyMap.map(TestUtils.createMessage(inputFields));
        
        // Invalid database should produce an error message
        List<DataQualityMessage> qualityMessages = output.getDataQualityMessages();
        Assertions.assertFalse(qualityMessages == null || qualityMessages.isEmpty());
        
        boolean hasError = qualityMessages.stream()
            .anyMatch(msg -> "ERROR".equals(msg.getLevel()));
        Assertions.assertTrue(hasError, "Expected at least one ERROR level message");
    }

    @Test
    void testThrowsCompanyDatabaseDoesNotExist() {
        String nonExistentPath = "./src/test/resources/geolite/doesntexist";
        File databaseFile = new File(nonExistentPath);
        Assertions.assertFalse(databaseFile.exists());
        
        IpCompanyMap map = new IpCompanyMap(nonExistentPath, ENRICH_FIELD_NAMES, null);
        
        assertThatThrownBy(() -> map.open(new Configuration()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Could not read company database %s", nonExistentPath);
    }

    @Test
    void testThrowsCompanyDatabaseEmptyFile() {
        String emptyFilePath = "./src/test/resources/geolite/invalid_maxmind_db.mmdb";
        File databaseFile = new File(emptyFilePath);
        Assertions.assertTrue(databaseFile.exists());
        Assertions.assertTrue(databaseFile.length() > 0);
        
        IpCompanyMap map = new IpCompanyMap(emptyFilePath, ENRICH_FIELD_NAMES, null);
        
        assertThatThrownBy(() -> map.open(new Configuration()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Could not read company database %s", emptyFilePath);
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
        inputFields.put(IP_FIELD_NAME, IpCompanyTestData.CLOUDFLARE_IP);
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
        
        // Empty string is a valid field value (but not a valid IP)
        Message output = companyMap.map(TestUtils.createMessage(inputFields));
        
        // Should complete without throwing
        Assertions.assertNotNull(output);
    }

    private void assertNoErrorsOrInfos(Message output) {
        List<DataQualityMessage> dataQualityMessages = output.getDataQualityMessages();
        Assertions.assertTrue(
            dataQualityMessages == null || dataQualityMessages.isEmpty(),
            "Expected no quality messages but got: " + dataQualityMessages
        );
    }
}
