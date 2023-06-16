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

package com.cloudera.cyber.profiler;

import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageConfig;
import com.cloudera.cyber.profiler.accumulator.ProfileGroupAcc;
import com.cloudera.cyber.profiler.accumulator.ProfileGroupConfigTestUtils;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import static com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageFormat.HBASE_METRON;
import static com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageFormat.HBASE_SIMPLE;
import static com.cloudera.cyber.profiler.FirstSeenHbaseMutationConverter.FIRST_SEEN_ENRICHMENT_TYPE;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.apache.metron.enrichment.converter.EnrichmentValue.VALUE_COLUMN_NAME;

public class FirstSeenHbaseMutationConverterTest {
    private static final String KEY_FIELD_1 = "key1";
    private static final String KEY_FIELD_1_VALUE = "value_1";
    private static final String KEY_FIELD_2 = "key2";
    private static final String KEY_FIELD_2_VALUE = "value_2";
    private static final ArrayList<String> KEY_FIELD_NAMES = Lists.newArrayList(KEY_FIELD_1, KEY_FIELD_2);
    private static final String FIRST_SEEN_MEASUREMENT_NAME = "first_seen";
    private static final String FIRST_SEEN_PROFILE_GROUP = "test_first_seen";
    private static final String FIRST_SEEN_COLUMN_FAMILY = "first_seen";
    private static final ProfileGroupConfig FIRST_SEEN_PROFILE = buildFirstSeenProfile();
    private static final String EXPECTED_START_PERIOD = "1618338583000";
    private static final String EXPECTED_END_PERIOD = "1618338590010";

    @Test
    public void testSimpleMutation() {

         EnrichmentStorageConfig enrichmentStorageConfig = new EnrichmentStorageConfig(HBASE_SIMPLE, "table", null);

         Mutation mutation = getMutation(enrichmentStorageConfig);

         String expectedRowKey = Joiner.on(":").join(FIRST_SEEN_PROFILE_GROUP, KEY_FIELD_1_VALUE, KEY_FIELD_2_VALUE);
         NavigableMap<byte[], List<Cell>> familyCellMap = mutation.getFamilyCellMap();
         Assert.assertEquals(2, familyCellMap.size());
         familyCellMap.forEach((k,v) -> v.forEach(c -> {{
             Assert.assertEquals(Cell.Type.Put, c.getType());
             String value = Bytes.toString(c.getValueArray(), c.getValueOffset(), c.getValueLength());
             String columnFamily = Bytes.toString(c.getFamilyArray(), c.getFamilyOffset(), c.getFamilyLength());
             String qualifier = Bytes.toString(c.getQualifierArray(), c.getQualifierOffset(), c.getQualifierLength());
             String rowKey = Bytes.toString(c.getRowArray(), c.getRowOffset(), c.getRowLength());

             Assert.assertEquals(expectedRowKey, rowKey);
             if (columnFamily.equals("id")) {
                 Assert.assertEquals("key", qualifier);
                 Assert.assertEquals(expectedRowKey, value);
             } else {
                 Assert.assertEquals(FIRST_SEEN_COLUMN_FAMILY, columnFamily);
                 if (qualifier.equals("firstSeen")) {
                     Assert.assertEquals(EXPECTED_START_PERIOD, value);
                 } else if (qualifier.equals("lastSeen")) {
                     Assert.assertEquals(EXPECTED_END_PERIOD, value);
                 } else {
                     Assert.fail(String.format("Unknown qualifier %s", qualifier));
                 }
             }
         }}));
    }

    @Test
    public void testMetronMutation() {

        String expectedColumnFamily = "fs";
        EnrichmentStorageConfig enrichmentStorageConfig = new EnrichmentStorageConfig(HBASE_METRON, "table", expectedColumnFamily);

        Mutation mutation = getMutation(enrichmentStorageConfig);
        String indicator = Joiner.on(":").join(FIRST_SEEN_PROFILE_GROUP, KEY_FIELD_1_VALUE, KEY_FIELD_2_VALUE);
        String expectedRowKey = Bytes.toString(new EnrichmentKey(FIRST_SEEN_ENRICHMENT_TYPE, indicator).toBytes());
        NavigableMap<byte[], List<Cell>> familyCellMap = mutation.getFamilyCellMap();
        Assert.assertEquals(1, familyCellMap.size());
        familyCellMap.forEach((k,v) -> v.forEach(c -> {{
            Assert.assertEquals(Cell.Type.Put, c.getType());
            String value = Bytes.toString(c.getValueArray(), c.getValueOffset(), c.getValueLength());
            String columnFamily = Bytes.toString(c.getFamilyArray(), c.getFamilyOffset(), c.getFamilyLength());
            String qualifier = Bytes.toString(c.getQualifierArray(), c.getQualifierOffset(), c.getQualifierLength());
            String rowKey = Bytes.toString(c.getRowArray(), c.getRowOffset(), c.getRowLength());

            Assert.assertEquals(expectedRowKey, rowKey);

            Assert.assertEquals(expectedColumnFamily, columnFamily);
            Assert.assertEquals(VALUE_COLUMN_NAME, qualifier);
            Assert.assertEquals(String.format("{\"firstSeen\":\"%s\",\"lastSeen\":\"%s\"}", EXPECTED_START_PERIOD, EXPECTED_END_PERIOD), value);
        }}));
    }

    private static ProfileGroupConfig buildFirstSeenProfile() {
        ArrayList<ProfileMeasurementConfig> measurements = Lists.newArrayList(
                ProfileGroupConfigTestUtils.createMeasurement(ProfileAggregationMethod.COUNT, "count", "field_to_count"),
                ProfileGroupConfigTestUtils.createMeasurement(ProfileAggregationMethod.FIRST_SEEN, FIRST_SEEN_MEASUREMENT_NAME, null),
                ProfileGroupConfigTestUtils.createMeasurement(ProfileAggregationMethod.SUM, "sum", "field_to_sum"));
        return ProfileGroupConfig.builder().keyFieldNames(KEY_FIELD_NAMES).
                periodDuration(1L).periodDurationUnit(MINUTES.name()).
                sources(Lists.newArrayList("ANY")).profileGroupName(FIRST_SEEN_PROFILE_GROUP).
                measurements(measurements).
                build();

    }

    private Mutation getMutation(EnrichmentStorageConfig enrichmentStorageConfig) {
        FirstSeenHbaseMutationConverter converter = new FirstSeenHbaseMutationConverter(enrichmentStorageConfig, FIRST_SEEN_PROFILE);

        converter.open();

        Map<String, String> extensions = ImmutableMap.of(ProfileGroupAcc.START_PERIOD_EXTENSION, EXPECTED_START_PERIOD,
                ProfileGroupAcc.END_PERIOD_EXTENSION, EXPECTED_END_PERIOD,
                KEY_FIELD_1, KEY_FIELD_1_VALUE,
                KEY_FIELD_2, KEY_FIELD_2_VALUE);

        return converter.convertToMutation(new ProfileMessage(MessageUtils.getCurrentTimestamp(), extensions));

    }
}
