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
import com.cloudera.cyber.enrichment.hbase.SimpleLookupKey;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageConfig;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageFormat;
import com.cloudera.cyber.hbase.LookupKey;
import com.cloudera.cyber.profiler.accumulator.ProfileGroupAcc;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.flink.metrics.SimpleCounter;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FirstSeenHbaseLookupTest extends FirstSeenHbaseLookup {

    private static final long CURRENT_TIMESTAMP = MessageUtils.getCurrentTimestamp();
    private static final String PROFILE_GROUP_NAME = "observation";
    private static final String FIRST_SEEN_RESULT_NAME = "first_observation";
    private static final String KEY_1 = "key_1";
    private static final String KEY_2 = "key_2";
    private static final String KEY_1_NEW_OBS = "new_key_1";
    private static final String KEY_2_NEW_OBS = "new_key_2";
    private static final String KEY_1_PREVIOUS_OBS = "prev_key_1";
    private static final String KEY_2_PREVIOUS_OBS = "prev_key_2";
    private static final long EXPECTED_PREVIOUS_OBSERVATION = CURRENT_TIMESTAMP - 100;
    private static final String EXPECTED_COLUMN_FAMILY = "first_seen";
    private static final String KEY_1_EXPIRED_OBS = "old_key_1";
    private static final String KEY_2_EXPIRED_OBS = "old_key_2";
    private static final String KEY_1_BAD_FIRST_SEEN = "bad_first_key_1";
    private static final String KEY_2_BAD_FIRST_SEEN = "bad_first_key_2";
    private static final String KEY_1_BAD_LAST_SEEN = "bad_last_key_1";
    private static final String KEY_2_BAD_LAST_SEEN = "bad_last_key_2";

    private static final Map<String, Map<String, Object>> mockHbaseResults = new HashMap<String, Map<String, Object>>() {{
        put(Joiner.on(":").join(PROFILE_GROUP_NAME, KEY_1_PREVIOUS_OBS, KEY_2_PREVIOUS_OBS), ImmutableMap.of(FIRST_SEEN_PROPERTY_NAME, Long.toString(EXPECTED_PREVIOUS_OBSERVATION),
                                                                                 LAST_SEEN_PROPERTY_NAME, Long.toString(CURRENT_TIMESTAMP - 50)));
        put(Joiner.on(":").join(PROFILE_GROUP_NAME, KEY_1_EXPIRED_OBS, KEY_2_EXPIRED_OBS), ImmutableMap.of(FIRST_SEEN_PROPERTY_NAME, Long.toString(CURRENT_TIMESTAMP - 1050),
                LAST_SEEN_PROPERTY_NAME, Long.toString(CURRENT_TIMESTAMP - 5006)));
        put(Joiner.on(":").join(PROFILE_GROUP_NAME, KEY_1_BAD_FIRST_SEEN, KEY_2_BAD_FIRST_SEEN), ImmutableMap.of(FIRST_SEEN_PROPERTY_NAME, "bad number format",
                LAST_SEEN_PROPERTY_NAME, Long.toString(CURRENT_TIMESTAMP - 5006)));
        put(Joiner.on(":").join(PROFILE_GROUP_NAME, KEY_1_BAD_LAST_SEEN, KEY_2_BAD_LAST_SEEN), ImmutableMap.of(FIRST_SEEN_PROPERTY_NAME, Long.toString(CURRENT_TIMESTAMP - 1050),
                LAST_SEEN_PROPERTY_NAME, "bad number format"));
    }};
    private static final String EXPECTED_HBASE_TABLE_NAME = "enrichments";

    public FirstSeenHbaseLookupTest() {
        super(new EnrichmentStorageConfig(EnrichmentStorageFormat.HBASE_SIMPLE, EXPECTED_HBASE_TABLE_NAME, EXPECTED_COLUMN_FAMILY), createProfileGroupConfig());
        messageCounter = new SimpleCounter();
    }

    @Test
    public void testNewObservation() {
        // never seen before - do not update first seen result
        verifyFirstSeen(KEY_1_NEW_OBS, KEY_2_NEW_OBS, CURRENT_TIMESTAMP - 5, 1, null);
    }

    @Test
    public void testPreviousObservation() {

        verifyFirstSeen(KEY_1_PREVIOUS_OBS, KEY_2_PREVIOUS_OBS, CURRENT_TIMESTAMP - 5, 0, Long.toString(EXPECTED_PREVIOUS_OBSERVATION));
    }

    @Test
    public void testExpiredObservation() {
        verifyFirstSeen(KEY_1_EXPIRED_OBS, KEY_2_EXPIRED_OBS, CURRENT_TIMESTAMP - 5, 1, null);
    }

    @Test
    public void testBadNewFirstSeenFormat() {
        verifyFirstSeen(KEY_1_NEW_OBS, KEY_2_NEW_OBS, "not a number", "1", null);
    }

    @Test
    public void testBadHbaseFirstSeenFormat() {
        verifyFirstSeen(KEY_1_BAD_FIRST_SEEN, KEY_2_BAD_FIRST_SEEN, CURRENT_TIMESTAMP - 5, 1, null);
    }

    @Test
    public void testBadHbaseLastSeenFormat() {
        verifyFirstSeen(KEY_1_BAD_LAST_SEEN, KEY_2_BAD_LAST_SEEN, CURRENT_TIMESTAMP - 5, 1, null);
    }

    private void verifyFirstSeen(String key1, String key2, String firstSeen, String expectedFirstSeen, String expectedFirstTimestamp) {
        ProfileMessage profileMessage = new ProfileMessage(CURRENT_TIMESTAMP, ImmutableMap.of(KEY_1, key1,
                KEY_2, key2,
                ProfileGroupAcc.START_PERIOD_EXTENSION, firstSeen));
        FirstSeenHbaseLookup lookup = new FirstSeenHbaseLookupTest();
        ProfileMessage result = lookup.map(profileMessage);
        Assert.assertEquals(expectedFirstSeen, result.getExtensions().get(FIRST_SEEN_RESULT_NAME));
        Assert.assertEquals(expectedFirstTimestamp, result.getExtensions().get(FIRST_SEEN_RESULT_NAME.concat(FIRST_SEEN_TIME_SUFFIX)));
    }

    private void verifyFirstSeen(String key1, String key2, long startPeriodTimestamp, int expectedFirstSeen, String expectedFirstTimestamp) {
        verifyFirstSeen(key1, key2, Long.toString(startPeriodTimestamp), Integer.toString(expectedFirstSeen), expectedFirstTimestamp);
    }

    private static ProfileGroupConfig createProfileGroupConfig() {
        ArrayList<ProfileMeasurementConfig> measurements = Lists.newArrayList(ProfileMeasurementConfig.builder().aggregationMethod(ProfileAggregationMethod.FIRST_SEEN).resultExtensionName(FIRST_SEEN_RESULT_NAME).
                firstSeenExpirationDuration(5L).firstSeenExpirationDurationUnit("SECONDS").build());
        return ProfileGroupConfig.builder().profileGroupName(PROFILE_GROUP_NAME).
                keyFieldNames(Lists.newArrayList(KEY_1, KEY_2)).periodDuration(1L).
                periodDurationUnit("SECONDS").sources(Lists.newArrayList("ANY")).measurements(measurements).build();
    }

    protected Map<String, Object> fetch(LookupKey key) {
        Assert.assertTrue(key instanceof SimpleLookupKey);
        Assert.assertEquals(EXPECTED_HBASE_TABLE_NAME, key.getTableName());
        Assert.assertEquals(EXPECTED_COLUMN_FAMILY, key.getCf());
        return mockHbaseResults.getOrDefault(key.getKey(), Collections.emptyMap());
    }
}
