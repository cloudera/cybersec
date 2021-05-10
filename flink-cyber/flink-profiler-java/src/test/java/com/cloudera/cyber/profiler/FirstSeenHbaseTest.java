package com.cloudera.cyber.profiler;

import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.hbase.LookupKey;
import com.cloudera.cyber.profiler.accumulator.ProfileGroupAcc;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.sun.imageio.plugins.common.ImageUtil;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Map;

import static com.cloudera.cyber.profiler.accumulator.ProfileGroupConfigTestUtils.createMeasurement;

public class FirstSeenHbaseTest {

    private static final String TABLE_NAME = "first_seen_table";
    private static final String COLUMN_FAMILY_NAME = "column_family";
    private static final String TEST_PROFILE_GROUP = "profile_group";
    private static final String KEY_1 = "key_1";
    private static final String KEY_2 = "key_2";
    private static final String KEY_1_VALUE = "value_1";
    private static final String KEY_2_VALUE = "value_2";
    private static final String FIRST_SEEN_RESULT_NAME = "key_first_seen";

    @Test
    public void testFirstSeenHbase() {
        ProfileGroupConfig profileGroupConfig = createProfileGroupConfig();
        FirstSeenHBase firstSeenHbase = new FirstSeenHBase(TABLE_NAME, COLUMN_FAMILY_NAME, profileGroupConfig);

        // test constructor correctness
        Assert.assertEquals(TABLE_NAME, firstSeenHbase.getTableName());
        Assert.assertArrayEquals(Bytes.toBytes(COLUMN_FAMILY_NAME), firstSeenHbase.getColumnFamilyName());
        Assert.assertEquals(FIRST_SEEN_RESULT_NAME, firstSeenHbase.getFirstSeenResultName());
        Assert.assertEquals(Lists.newArrayList(KEY_1, KEY_2), firstSeenHbase.getKeyFieldNames());
        Assert.assertEquals(TEST_PROFILE_GROUP, firstSeenHbase.getProfileName());

        long endPeriod = MessageUtils.getCurrentTimestamp();
        long startPeriod = endPeriod - 500L;
        Map<String, String> extensions = ImmutableMap.of(KEY_1, KEY_1_VALUE, KEY_2, KEY_2_VALUE,
                ProfileGroupAcc.START_PERIOD_EXTENSION, Long.toString(startPeriod), ProfileGroupAcc.END_PERIOD_EXTENSION, Long.toString(endPeriod));

        ProfileMessage profileMessage = new ProfileMessage(endPeriod, extensions);
        LookupKey key = firstSeenHbase.getKey(profileMessage);
        Assert.assertArrayEquals(Bytes.toBytes(COLUMN_FAMILY_NAME), key.getCf());
        Assert.assertArrayEquals(Bytes.toBytes(Joiner.on(":").join(TEST_PROFILE_GROUP, KEY_1_VALUE, KEY_2_VALUE)), key.getKey());

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
