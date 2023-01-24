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

package com.cloudera.cyber.enrichment.cidr;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.TestUtils;
import com.cloudera.cyber.enrichment.Enrichment;
import com.cloudera.cyber.enrichment.geocode.IpRegionMap;
import com.cloudera.cyber.enrichment.geocode.impl.IpRegionCidrEnrichment;
import com.cloudera.cyber.enrichment.geocode.impl.types.RegionCidrEnrichmentConfiguration;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)

public class IpRegionCidrTestMap {

    private static final String SINGLE_IP_FIELD_NAME = "ip_dst_addr";
    private static final String LIST_IPS_FIELD_NAME = "dns.answers";
    private static final List<String> ENRICH_FIELD_NAMES = Arrays.asList(SINGLE_IP_FIELD_NAME, LIST_IPS_FIELD_NAME);
    private IpRegionMap regionMap;

    @Mock
    private IpRegionCidrEnrichment regionCidrEnrichment;

    @Before
    public void createGeoMap() throws Exception {
        regionMap = new IpRegionMap(new RegionCidrEnrichmentConfiguration(), ENRICH_FIELD_NAMES);
        FieldUtils.writeField(regionMap, "regionCidrEnrichment", regionCidrEnrichment, true);
    }

    @Test
    public void testIpMappedSuccessfully() throws Exception {
        Map<String, String> inputFields = new HashMap<>();
        inputFields.put(SINGLE_IP_FIELD_NAME, IpRegionCidrTestData.IPV4_10_ADDRESS);
        String key = SINGLE_IP_FIELD_NAME + Enrichment.DELIMITER + IpRegionCidrEnrichment.FEATURE_NAME;


        doAnswer(invocationOnMock -> {
            Map<String, String> extensions = invocationOnMock.getArgument(2);
            extensions.put(key, IpRegionCidrTestData.IPV4_MASK_REGION_1_NAME);
            return null;
        }).when(regionCidrEnrichment).lookup(eq(SINGLE_IP_FIELD_NAME), eq(IpRegionCidrTestData.IPV4_10_ADDRESS), anyMap(), anyList());

        Message result = regionMap.map(TestUtils.createMessage(inputFields));

        verify(regionCidrEnrichment).lookup(eq(SINGLE_IP_FIELD_NAME), eq(IpRegionCidrTestData.IPV4_10_ADDRESS), anyMap(), anyList());


        assertThat(result).extracting(Message::getExtensions)
                .isNotNull()
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .isNotEmpty()
                .containsOnly(
                        entry(SINGLE_IP_FIELD_NAME, IpRegionCidrTestData.IPV4_10_ADDRESS),
                        entry(key, IpRegionCidrTestData.IPV4_MASK_REGION_1_NAME)
                );

        assertThat(result).extracting(Message::getDataQualityMessages)
                .isNotNull()
                .asInstanceOf(InstanceOfAssertFactories.LIST)
                .isEmpty();
    }

    @Test
    public void testIpMappedUnSuccessfully() throws Exception {
        String featureName = "featureName";
        String fieldName = "fieldName";
        String message = "message";
        Map<String, String> inputFields = new HashMap<>();
        inputFields.put(SINGLE_IP_FIELD_NAME, IpRegionCidrTestData.IPV4_10_ADDRESS);

        doAnswer(invocationOnMock -> {
            List<DataQualityMessage> dataQualityMessages = invocationOnMock.getArgument(3);

            dataQualityMessages.add(DataQualityMessage.builder().feature(featureName).field(fieldName).message(message).build());
            return null;
        }).when(regionCidrEnrichment).lookup(eq(SINGLE_IP_FIELD_NAME), eq(IpRegionCidrTestData.IPV4_10_ADDRESS), anyMap(), anyList());

        Message result = regionMap.map(TestUtils.createMessage(inputFields));

        verify(regionCidrEnrichment).lookup(eq(SINGLE_IP_FIELD_NAME), eq(IpRegionCidrTestData.IPV4_10_ADDRESS), anyMap(), anyList());


        assertThat(result).extracting(Message::getExtensions)
                .isNotNull()
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .isNotEmpty()
                .containsOnly(
                        entry(SINGLE_IP_FIELD_NAME, IpRegionCidrTestData.IPV4_10_ADDRESS)
                );


        assertThat(result).extracting(Message::getDataQualityMessages)
                .isNotNull()
                .asInstanceOf(InstanceOfAssertFactories.LIST)
                .extracting("feature", "field", "message")
                .contains(tuple(featureName, fieldName, message));

    }


}
