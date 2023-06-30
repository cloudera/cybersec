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
import com.cloudera.cyber.hbase.LookupKey;
import com.cloudera.cyber.profiler.accumulator.ProfileGroupAcc;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Map;

import static com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageFormat.HBASE_SIMPLE;
import static com.cloudera.cyber.profiler.FirstSeenHbaseLookup.FIRST_SEEN_ENRICHMENT_TYPE;
import static com.cloudera.cyber.profiler.accumulator.ProfileGroupConfigTestUtils.createMeasurement;

public class FirstSeenHbaseTest {

    private static final String TABLE_NAME = "first_seen_table";
    private static final String TEST_PROFILE_GROUP = "profile_group";
    private static final String KEY_1 = "key_1";
    private static final String KEY_2 = "key_2";
    private static final String KEY_1_VALUE = "value_1";
    private static final String KEY_2_VALUE = "value_2";
    private static final String FIRST_SEEN_RESULT_NAME = "key_first_seen";

    @Test
    public void testFirstSeenHbase() {
        ProfileGroupConfig profileGroupConfig = createProfileGroupConfig();
        EnrichmentStorageConfig enrichmentStorageConfig = new EnrichmentStorageConfig(HBASE_SIMPLE, TABLE_NAME, null);
        FirstSeenHBase firstSeenHbase = new FirstSeenHBase(enrichmentStorageConfig, profileGroupConfig);

        // test constructor correctness
        Assert.assertEquals(TABLE_NAME, firstSeenHbase.getEnrichmentStorageConfig().getHbaseTableName());
        Assert.assertNull(firstSeenHbase.getEnrichmentStorageConfig().getColumnFamily());
        Assert.assertEquals(FIRST_SEEN_RESULT_NAME, firstSeenHbase.getFirstSeenResultName());
        Assert.assertEquals(Lists.newArrayList(KEY_1, KEY_2), firstSeenHbase.getKeyFieldNames());
        Assert.assertEquals(TEST_PROFILE_GROUP, firstSeenHbase.getProfileName());

        long endPeriod = MessageUtils.getCurrentTimestamp();
        long startPeriod = endPeriod - 500L;
        Map<String, String> extensions = ImmutableMap.of(KEY_1, KEY_1_VALUE, KEY_2, KEY_2_VALUE,
                ProfileGroupAcc.START_PERIOD_EXTENSION, Long.toString(startPeriod), ProfileGroupAcc.END_PERIOD_EXTENSION, Long.toString(endPeriod));

        ProfileMessage profileMessage = new ProfileMessage(endPeriod, extensions);
        LookupKey key = firstSeenHbase.getKey(profileMessage);
        Assert.assertEquals(FIRST_SEEN_ENRICHMENT_TYPE, key.getCf());
        Assert.assertEquals(Joiner.on(":").join(TEST_PROFILE_GROUP, KEY_1_VALUE, KEY_2_VALUE), key.getKey());

        Assert.assertEquals(Long.toString(startPeriod), firstSeenHbase.getFirstSeen(profileMessage));
        Assert.assertEquals(Long.toString(endPeriod), firstSeenHbase.getLastSeen(profileMessage));

    }

    private static ProfileGroupConfig createProfileGroupConfig() {
        ArrayList<ProfileMeasurementConfig> measurements = Lists.newArrayList(createMeasurement(ProfileAggregationMethod.FIRST_SEEN, FIRST_SEEN_RESULT_NAME, null));

        return ProfileGroupConfig.builder().
                profileGroupName(TEST_PROFILE_GROUP).keyFieldNames(Lists.newArrayList(KEY_1, KEY_2)).
                periodDuration(5L).periodDurationUnit("MINUTES").
                sources(Lists.newArrayList("ANY")).measurements(measurements).build();
    }
}
