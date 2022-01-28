package com.cloudera.cyber.enrichemnt.stellar;

import com.cloudera.cyber.enrichemnt.stellar.functions.GeoEnrichmentFunctions;
import com.cloudera.cyber.enrichment.Enrichment;
import com.cloudera.cyber.enrichment.MetronGeoEnrichment;
import com.cloudera.cyber.enrichment.geocode.impl.IpGeoEnrichment;
import com.cloudera.cyber.enrichment.geocode.impl.types.MetronGeoEnrichmentFields;
import com.google.common.collect.ImmutableList;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GeoGetEnrichmentTest {
    private static final String IP = "10.0.0.1";
    private static final String TEST_RESULT_KEY = "key";
    private static final String TEST_RESULT_VALUE = "value";
    private static final String TEST_ENRICHMENT_FIELD_NAME = "fieldName";
    private static final String TEST_ENRICHMENT_FEATURE = "feature";
    private static final MetronGeoEnrichmentFields[] ARRAY_METRON_ENRICHMENT_FIELDS = {MetronGeoEnrichmentFields.CITY, MetronGeoEnrichmentFields.COUNTRY, MetronGeoEnrichmentFields.LOC_ID, MetronGeoEnrichmentFields.LATITUDE};
    private static final List<String> LIST_METRON_ENRICHMENTS_FIELDS = Arrays.stream(ARRAY_METRON_ENRICHMENT_FIELDS).map(MetronGeoEnrichmentFields::getSingularName).collect(Collectors.toList());

    @InjectMocks
    @Spy
    private GeoEnrichmentFunctions.GeoGet geoGet;
    @Mock
    private IpGeoEnrichment ipGeoEnrichment;
    @Captor
    private ArgumentCaptor<BiFunction<String, String, Enrichment>> enrichCreationCapture;

    @Before
    public void createGeoMap() {

    }

    @Test
    public void testNotInitializedGeoGet() {
        doReturn(false).when(geoGet).isInitialized();

        Object result = geoGet.apply(null, null);

        verifyNoMoreInteractions(ipGeoEnrichment);

        assertThat(result).isNull();
    }

    @Test
    public void testToManyArgumentPassed() {
        doReturn(true).when(geoGet).isInitialized();

        assertThatThrownBy(() -> {
            geoGet.apply(ImmutableList.of(IP, "wrong argument 1", "wrong argument 2", "wrong argument 3"), null);
        }).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("GEO_GET received more arguments than expected").hasMessageContaining("4");
    }

    @Test
    public void testReturnNullIfIpNull() {
        doReturn(true).when(geoGet).isInitialized();
        ArrayList<Object> args = new ArrayList<>();
        args.add("");
        Object result = geoGet.apply(args, null);

        verifyNoMoreInteractions(ipGeoEnrichment);

        assertThat(result).isNull();
    }



    @Test
    public void testReturnNullIfIpEmpty() {
        doReturn(true).when(geoGet).isInitialized();

        Object result = geoGet.apply(Collections.emptyList(), null);

        verifyNoMoreInteractions(ipGeoEnrichment);

        assertThat(result).isNull();
    }

    @Test
    public void testReturnNullIfSecondArgumentIsIncorrect() {
        doReturn(true).when(geoGet).isInitialized();
        ArrayList<Object> args = new ArrayList<>();
        args.add(IP);
        args.add(IP);
        Object result = geoGet.apply(args, null);

        verifyNoMoreInteractions(ipGeoEnrichment);

        assertThat(result).isNull();
    }

    @Test
    public void testSuccessfulGeoGetAllArgs() {
        doReturn(true).when(geoGet).isInitialized();
        doAnswer(invocationOnMock -> {
            Object argument = invocationOnMock.getArgument(4);
            assertThat(argument).isNotNull().asInstanceOf(InstanceOfAssertFactories.MAP).isEmpty();
            ((Map<String, String>) argument).put(TEST_RESULT_KEY, TEST_RESULT_VALUE);
            return null;
        }).when(ipGeoEnrichment).lookup(any(), isNull(), eq(IP), eq(MetronGeoEnrichmentFields.values()), anyMap(), anyList());

        Object result = geoGet.apply(ImmutableList.of(IP), null);

        verify(ipGeoEnrichment).lookup(enrichCreationCapture.capture(), isNull(), eq(IP), eq(MetronGeoEnrichmentFields.values()), anyMap(), anyList());
        verifyNoMoreInteractions(ipGeoEnrichment);

        assertThat(result).isNotNull().asInstanceOf(InstanceOfAssertFactories.MAP).contains(entry(TEST_RESULT_KEY, TEST_RESULT_VALUE));
        Enrichment enrichment = enrichCreationCapture.getValue().apply(TEST_ENRICHMENT_FIELD_NAME, TEST_ENRICHMENT_FEATURE);
        assertThat(enrichment).isInstanceOf(MetronGeoEnrichment.class);
    }

    @Test
    public void testSuccessfulGeoGetSomeArgs() {

        doReturn(true).when(geoGet).isInitialized();
        doAnswer(invocationOnMock -> {
            Object argument = invocationOnMock.getArgument(4);
            assertThat(argument).isNotNull().asInstanceOf(InstanceOfAssertFactories.MAP).isEmpty();
            ((Map<String, String>) argument).put(TEST_RESULT_KEY, TEST_RESULT_VALUE);
            return null;
        }).when(ipGeoEnrichment).lookup(any(), isNull(), eq(IP), eq(ARRAY_METRON_ENRICHMENT_FIELDS), anyMap(), anyList());

        Object result = geoGet.apply(ImmutableList.of(IP, LIST_METRON_ENRICHMENTS_FIELDS), null);

        verify(ipGeoEnrichment).lookup(enrichCreationCapture.capture(), isNull(), eq(IP), eq(ARRAY_METRON_ENRICHMENT_FIELDS), anyMap(), anyList());
        verifyNoMoreInteractions(ipGeoEnrichment);

        assertThat(result).isNotNull().asInstanceOf(InstanceOfAssertFactories.MAP).contains(entry(TEST_RESULT_KEY, TEST_RESULT_VALUE));
        Enrichment enrichment = enrichCreationCapture.getValue().apply(TEST_ENRICHMENT_FIELD_NAME, TEST_ENRICHMENT_FEATURE);
        assertThat(enrichment).isInstanceOf(MetronGeoEnrichment.class);
    }


}