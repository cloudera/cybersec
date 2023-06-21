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

package com.cloudera.cyber.enrichemnt.stellar;

import com.cloudera.cyber.enrichemnt.stellar.functions.GeoEnrichmentFunctions;
import com.cloudera.cyber.enrichment.Enrichment;
import com.cloudera.cyber.enrichment.MetronGeoEnrichment;
import com.cloudera.cyber.enrichment.geocode.impl.IpAsnEnrichment;
import com.cloudera.cyber.enrichment.geocode.impl.types.MetronGeoEnrichmentFields;
import com.google.common.collect.ImmutableList;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AsnGetEnrichmentTest {
    private static final String IP = "10.0.0.1";
    private static final String TEST_RESULT_KEY = "key";
    private static final String TEST_RESULT_VALUE = "value";
    private static final String TEST_ENRICHMENT_FIELD_NAME = "fieldName";
    private static final String TEST_ENRICHMENT_FEATURE = "feature";



    @Captor
    private ArgumentCaptor<BiFunction<String, String, Enrichment>> enrichCreationCapture;

    @InjectMocks
    @Spy
    private GeoEnrichmentFunctions.AsnGet asnGet;
    @Mock
    private IpAsnEnrichment ipAsnEnrichment;


    @Test
    public void testNotInitializedAsnGet() {
        Mockito.doReturn(false).when(asnGet).isInitialized();
        Object result = asnGet.apply(null, null);
        verifyNoMoreInteractions(ipAsnEnrichment);
        assertThat(result).isNull();
    }

    @Test
    public void testToManyArgumentPassed() {
        doReturn(true).when(asnGet).isInitialized();

        assertThatThrownBy(() -> {
            asnGet.apply(ImmutableList.of(IP, "wrong argument 1", "wrong argument 2"), null);
        }).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ASN_GET received more arguments than expected").hasMessageContaining("3");
    }

    @Test
    public void testReturnNullIfIpNull() {
        doReturn(true).when(asnGet).isInitialized();
        ArrayList<Object> args = new ArrayList<>();
        args.add("");
        Object result = asnGet.apply(args, null);

        verifyNoMoreInteractions(ipAsnEnrichment);

        assertThat(result).isNull();
    }


    @Test
    public void testReturnNullIfIpEmpty() {
        doReturn(true).when(asnGet).isInitialized();

        Object result = asnGet.apply(Collections.emptyList(), null);

        verifyNoMoreInteractions(ipAsnEnrichment);

        assertThat(result).isNull();
    }

    @Test
    public void testReturnNullIfSecondArgumentIsIncorrect() {
        doReturn(true).when(asnGet).isInitialized();
        ArrayList<Object> args = new ArrayList<>();
        args.add(IP);
        args.add(IP);
        Object result = asnGet.apply(args, null);

        verifyNoMoreInteractions(ipAsnEnrichment);

        assertThat(result).isNull();
    }

    @Test
    public void testSuccessfulAsnGetAllArgs() {
        doReturn(true).when(asnGet).isInitialized();

        doAnswer(invocationOnMock -> {
            Object argument = invocationOnMock.getArgument(3);
            assertThat(argument).isNotNull().asInstanceOf(InstanceOfAssertFactories.MAP).isEmpty();
            ((Map<String, String>) argument).put(TEST_RESULT_KEY, TEST_RESULT_VALUE);
            return null;
        }).when(ipAsnEnrichment).lookup(any(), isNull(), eq(IP), anyMap(), isNull());

        Object result = asnGet.apply(ImmutableList.of(IP), null);

        verify(ipAsnEnrichment).lookup(enrichCreationCapture.capture(), isNull(), eq(IP), anyMap(), isNull());
        verifyNoMoreInteractions(ipAsnEnrichment);
                                                   assertThat(result).isNotNull().asInstanceOf(InstanceOfAssertFactories.MAP).contains(entry(TEST_RESULT_KEY, TEST_RESULT_VALUE));
        Enrichment enrichment = enrichCreationCapture.getValue().apply(TEST_ENRICHMENT_FIELD_NAME, TEST_ENRICHMENT_FEATURE);
        assertThat(enrichment).isInstanceOf(MetronGeoEnrichment.class);
    }

    @Test
    public void testSuccessfulAsnGetIfFieldsNotPresentInDb() {
        doReturn(true).when(asnGet).isInitialized();
        doAnswer(invocationOnMock -> {
            Object argument = invocationOnMock.getArgument(3);
            assertThat(argument).isNotNull().asInstanceOf(InstanceOfAssertFactories.MAP).isEmpty();
            ((Map<String, String>) argument).put(TEST_RESULT_KEY, TEST_RESULT_VALUE);
            return null;
        }).when(ipAsnEnrichment).lookup(any(), isNull(), eq(IP), anyMap(), isNull());

        Object result = asnGet.apply(ImmutableList.of(IP, Collections.singletonList(MetronGeoEnrichmentFields.CITY.getSingularName())), null);

        verify(ipAsnEnrichment).lookup(enrichCreationCapture.capture(), isNull(), eq(IP), anyMap(), isNull());
        verifyNoMoreInteractions(ipAsnEnrichment);

        assertThat(result).isNull();
    }

    @Test
    public void testSuccessfulAsnGetIfOneFieldsPresentInDb() {
        String metronSingularName = MetronGeoEnrichmentFields.CITY.getSingularName();

        doReturn(true).when(asnGet).isInitialized();
        doAnswer(invocationOnMock -> {
            Object argument = invocationOnMock.getArgument(3);
            assertThat(argument).isNotNull().asInstanceOf(InstanceOfAssertFactories.MAP).isEmpty();
            ((Map<String, String>) argument).put(metronSingularName, TEST_RESULT_VALUE);
            return null;
        }).when(ipAsnEnrichment).lookup(any(), isNull(), eq(IP), anyMap(), isNull());

        Object result = asnGet.apply(ImmutableList.of(IP, Collections.singletonList(metronSingularName)), null);

        verify(ipAsnEnrichment).lookup(enrichCreationCapture.capture(), isNull(), eq(IP), anyMap(), isNull());
        verifyNoMoreInteractions(ipAsnEnrichment);

        assertThat(result).isNotNull().asInstanceOf(InstanceOfAssertFactories.STRING).isEqualTo(TEST_RESULT_VALUE);
    }

    @Test
    public void testSuccessfulAsnGetIfSpecificFieldsPresentInDb() {

        doReturn(true).when(asnGet).isInitialized();
        doAnswer(invocationOnMock -> {
            Object argument = invocationOnMock.getArgument(3);
            assertThat(argument).isNotNull().asInstanceOf(InstanceOfAssertFactories.MAP).isEmpty();
            Map<String, String> resultMap = (Map<String, String>) argument;
            Arrays.stream(MetronGeoEnrichmentFields.values()).forEach(field -> {
                resultMap.put(field.getSingularName(), field.getSingularName() + TEST_RESULT_VALUE);
            });
            return null;
        }).when(ipAsnEnrichment).lookup(any(), isNull(), eq(IP), anyMap(), isNull());

        final String MetronEnrichmentCity = MetronGeoEnrichmentFields.CITY.getSingularName();
        final String MetronEnrichmentCountry = MetronGeoEnrichmentFields.COUNTRY.getSingularName();
        Object result = asnGet.apply(ImmutableList.of(IP, ImmutableList.of(MetronEnrichmentCity, MetronEnrichmentCountry)), null);

        verify(ipAsnEnrichment).lookup(enrichCreationCapture.capture(), isNull(), eq(IP), anyMap(), isNull());
        verifyNoMoreInteractions(ipAsnEnrichment);

        assertThat(result).isNotNull()
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .contains(entry(MetronEnrichmentCity, MetronEnrichmentCity + TEST_RESULT_VALUE),
                        entry(MetronEnrichmentCountry, MetronEnrichmentCountry + TEST_RESULT_VALUE));
        ;
    }


}