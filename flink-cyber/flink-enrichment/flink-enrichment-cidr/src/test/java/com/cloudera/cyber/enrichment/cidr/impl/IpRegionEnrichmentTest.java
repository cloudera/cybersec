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

package com.cloudera.cyber.enrichment.cidr.impl;

import static com.cloudera.cyber.enrichment.geocode.impl.IpRegionCidrEnrichment.FEATURE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.enrichment.Enrichment;
import com.cloudera.cyber.enrichment.cidr.IpRegionCidrTestData;
import com.cloudera.cyber.enrichment.geocode.impl.IpRegionCidrEnrichment;
import com.google.common.collect.ImmutableMap;
import inet.ipaddr.IPAddressString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class IpRegionEnrichmentTest {


    private static final String IP_FIELD_NAME = "cidrTest";
    private static final String IP_FIELD_NAME_EXTENSION = IP_FIELD_NAME + Enrichment.DELIMITER + FEATURE_NAME;
    private IpRegionCidrEnrichment ipRegionCidrEnrichment;

    @Before
    public void createAsnEnrichment() {
        Map<IPAddressString, String> map = ImmutableMap.of(
                new IPAddressString(IpRegionCidrTestData.IPV4_MASK_REGION_1), IpRegionCidrTestData.IPV4_MASK_REGION_1_NAME,
                new IPAddressString(IpRegionCidrTestData.IPV4_SHORT_MASK_REGION), IpRegionCidrTestData.IPV4_SHORT_MASK_REGION_NAME,
                new IPAddressString(IpRegionCidrTestData.IPV6_MASK_REGION_2), IpRegionCidrTestData.IPV6_MASK_REGION_2_NAME
        );
        ipRegionCidrEnrichment = new IpRegionCidrEnrichment(map);
    }

    @Test
    public void testIPv4Mapped() {
        List<DataQualityMessage> dataQualityMessages = new ArrayList<>();
        Map<String, String> extensions = new HashMap<>();

        ipRegionCidrEnrichment.lookup(IP_FIELD_NAME, IpRegionCidrTestData.IPV4_10_ADDRESS, extensions, dataQualityMessages);

        assertThat(extensions).isNotEmpty().containsOnly(entry(IP_FIELD_NAME_EXTENSION, IpRegionCidrTestData.IPV4_MASK_REGION_1_NAME));
        assertThat(dataQualityMessages).isEmpty();
    }

    @Test
    public void testIPv6Mapped() {
        List<DataQualityMessage> dataQualityMessages = new ArrayList<>();
        Map<String, String> extensions = new HashMap<>();

        ipRegionCidrEnrichment.lookup(IP_FIELD_NAME, IpRegionCidrTestData.IPV6_15_ADDRESS, extensions, dataQualityMessages);

        assertThat(extensions).isNotEmpty().containsOnly(entry(IP_FIELD_NAME_EXTENSION, IpRegionCidrTestData.IPV6_MASK_REGION_2_NAME));
        assertThat(dataQualityMessages).isEmpty();
    }
    @Test
    public void testPreferShortedSubnet() {
        List<DataQualityMessage> dataQualityMessages = new ArrayList<>();
        Map<String, String> extensions = new HashMap<>();

        ipRegionCidrEnrichment.lookup(IP_FIELD_NAME, IpRegionCidrTestData.IPV4_10_SHORT_ADDRESS, extensions, dataQualityMessages);

        assertThat(extensions).isNotEmpty().containsOnly(entry(IP_FIELD_NAME_EXTENSION, IpRegionCidrTestData.IPV4_SHORT_MASK_REGION_NAME));
        assertThat(dataQualityMessages).isEmpty();
    }

    @Test
    public void testIPv4ForIPv6MaskMapped() {
        List<DataQualityMessage> dataQualityMessages = new ArrayList<>();
        Map<String, String> extensions = new HashMap<>();

        ipRegionCidrEnrichment.lookup(IP_FIELD_NAME, IpRegionCidrTestData.IPV4_15_ADDRESS, extensions, dataQualityMessages);

        assertThat(extensions).isNotEmpty().containsOnly(entry(IP_FIELD_NAME_EXTENSION, IpRegionCidrTestData.IPV6_MASK_REGION_2_NAME));
        assertThat(dataQualityMessages).isEmpty();
    }

    @Test
    public void testIPv6ForIPv4MaskMapped() {
        List<DataQualityMessage> dataQualityMessages = new ArrayList<>();
        Map<String, String> extensions = new HashMap<>();

        ipRegionCidrEnrichment.lookup(IP_FIELD_NAME, IpRegionCidrTestData.IPV6_10_ADDRESS, extensions, dataQualityMessages);

        assertThat(extensions).isNotEmpty().containsOnly(entry(IP_FIELD_NAME_EXTENSION, IpRegionCidrTestData.IPV4_MASK_REGION_1_NAME));
        assertThat(dataQualityMessages).isEmpty();
    }

    @Test
    public void testIPNotMapped() {
        List<DataQualityMessage> dataQualityMessages = new ArrayList<>();
        Map<String, String> extensions = new HashMap<>();

        ipRegionCidrEnrichment.lookup(IP_FIELD_NAME, IpRegionCidrTestData.IPV4_ADDRESS_OUT_RANGE, extensions, dataQualityMessages);

        assertThat(extensions).isEmpty();
        assertThat(dataQualityMessages).isEmpty();
    }

    @Test
    public void testIncorrectIPMapped() {
        List<DataQualityMessage> dataQualityMessages = new ArrayList<>();
        Map<String, String> extensions = new HashMap<>();

        ipRegionCidrEnrichment.lookup(IP_FIELD_NAME, "ip", extensions, dataQualityMessages);

        assertThat(extensions).isEmpty();
        assertThat(dataQualityMessages).isNotEmpty();
    }


}
