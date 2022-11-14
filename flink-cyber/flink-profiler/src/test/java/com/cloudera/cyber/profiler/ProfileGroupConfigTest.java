package com.cloudera.cyber.profiler;

import org.apache.commons.compress.utils.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ProfileGroupConfigTest {
    private static final ArrayList<String> TEST_SOURCES =  new ArrayList<>(Collections.singletonList("test_source"));
    private static final ArrayList<String> ANY_SOURCES =  new ArrayList<>(Collections.singletonList("ANY"));
    private static final ArrayList<String> TEST_KEY_FIELDS =  new ArrayList<>(Collections.singletonList("key_field"));
    private static final ProfileMeasurementConfig GOOD_MEASUREMENT = ProfileMeasurementConfig.builder().
            aggregationMethod(ProfileAggregationMethod.MAX).fieldName("field").resultExtensionName("field_count").build();
    private static final ProfileMeasurementConfig GOOD_STATS_MEASUREMENT = ProfileMeasurementConfig.builder().
            aggregationMethod(ProfileAggregationMethod.MAX).fieldName("field").resultExtensionName("field_count").
            calculateStats(true).build();
    private static final ProfileMeasurementConfig GOOD_FIRST_SEEN_MEASUREMENT = ProfileMeasurementConfig.builder().
            aggregationMethod(ProfileAggregationMethod.FIRST_SEEN).fieldName("field").resultExtensionName("first_field").build();

    private static final ArrayList<ProfileMeasurementConfig> MEASUREMENTS = new ArrayList<>(Collections.singletonList(GOOD_MEASUREMENT));
    private static final ArrayList<ProfileMeasurementConfig> MEASUREMENTS_WITH_STATS = new ArrayList<>(Collections.singletonList(GOOD_STATS_MEASUREMENT));
    private static final ArrayList<ProfileMeasurementConfig> MEASUREMENTS_WITH_FIRST = new ArrayList<>(Collections.singletonList(GOOD_FIRST_SEEN_MEASUREMENT));
    private static final ArrayList<String> EMPTY_ARRAY_LIST = Lists.newArrayList();

    @Test
    public void testGoodProfile() {
        ProfileGroupConfig goodNoStatsProfile = ProfileGroupConfig.builder().
                profileGroupName("good_name").sources(TEST_SOURCES).
                keyFieldNames(TEST_KEY_FIELDS).measurements(MEASUREMENTS).
                periodDuration(20L).periodDurationUnit(TimeUnit.HOURS.name()).
                build();
        goodNoStatsProfile.verify();
        Assert.assertFalse(goodNoStatsProfile.hasStats());
        Assert.assertFalse(goodNoStatsProfile.hasFirstSeen());
        Assert.assertEquals(Collections.singletonList("field"), goodNoStatsProfile.getMeasurementFieldNames());

        ProfileGroupConfig goodStatsProfile = ProfileGroupConfig.builder().
                profileGroupName("good_name").sources(TEST_SOURCES).
                keyFieldNames(TEST_KEY_FIELDS).measurements(MEASUREMENTS_WITH_STATS).
                periodDuration(20L).periodDurationUnit(TimeUnit.HOURS.name()).
                statsSlide(10L).statsSlideUnit(TimeUnit.HOURS.name()).
                build();
        goodStatsProfile.verify();
        Assert.assertTrue(goodStatsProfile.hasStats());
        Assert.assertFalse(goodStatsProfile.hasFirstSeen());

        ProfileGroupConfig goodFirstSeen = ProfileGroupConfig.builder().
                profileGroupName("good_name").sources(TEST_SOURCES).
                keyFieldNames(TEST_KEY_FIELDS).measurements(MEASUREMENTS_WITH_FIRST).
                periodDuration(20L).periodDurationUnit(TimeUnit.HOURS.name()).
                build();
        goodFirstSeen.verify();
        Assert.assertFalse(goodFirstSeen.hasStats());
        Assert.assertTrue(goodFirstSeen.hasFirstSeen());
    }

    @Test
    public void testNeedsSourceFilter() {
        ProfileGroupConfig profileWithoutANYSource = ProfileGroupConfig.builder().
                profileGroupName("good_name").sources(TEST_SOURCES).
                keyFieldNames(TEST_KEY_FIELDS).measurements(MEASUREMENTS).
                periodDuration(20L).periodDurationUnit(TimeUnit.HOURS.name()).
                build();
        profileWithoutANYSource.verify();
        Assert.assertTrue(profileWithoutANYSource.needsSourceFilter());

        ProfileGroupConfig profileWithANYSource = ProfileGroupConfig.builder().
                profileGroupName("good_name").sources(ANY_SOURCES).
                keyFieldNames(TEST_KEY_FIELDS).measurements(MEASUREMENTS).
                periodDuration(20L).periodDurationUnit(TimeUnit.HOURS.name()).
                build();
        Assert.assertFalse(profileWithANYSource.needsSourceFilter());
    }

    @Test
    public void testMissingProfileGroupName() {
        ProfileGroupConfig nullNameConfig = ProfileGroupConfig.builder().build();
        testNullField(nullNameConfig, ProfileGroupConfig.NULL_PROFILE_GROUP_NAME_ERROR);

        ProfileGroupConfig emptyNameConfig = ProfileGroupConfig.builder().profileGroupName("").build();
        testIllegalField(emptyNameConfig, ProfileGroupConfig.EMPTY_PROFILE_GROUP_NAME_ERROR);
    }

    @Test
    public void testEmptySources() {
        String profileGroupName = "testSources";
        ProfileGroupConfig nullSources = ProfileGroupConfig.builder().profileGroupName(profileGroupName).build();
        testNullField(nullSources, String.format(ProfileGroupConfig.EMPTY_SOURCES_ERROR, profileGroupName, "null"));

        ProfileGroupConfig emptySources = ProfileGroupConfig.builder().profileGroupName(profileGroupName).
                sources(Lists.newArrayList()).build();
        testIllegalArgument(emptySources, String.format(ProfileGroupConfig.EMPTY_SOURCES_ERROR, profileGroupName, "empty"));
    }

    @Test
    public void testEmptyKeyFields() {
        String profileGroupName = "testKeyFields";

        ProfileGroupConfig nullKeyFields = ProfileGroupConfig.builder().profileGroupName(profileGroupName).
                sources(TEST_SOURCES).build();
        testNullField(nullKeyFields, String.format(ProfileGroupConfig.EMPTY_KEY_FIELDS_ERROR, profileGroupName, "null"));

        ProfileGroupConfig emptyKeyFields = ProfileGroupConfig.builder().profileGroupName(profileGroupName).
                sources(TEST_SOURCES).keyFieldNames(EMPTY_ARRAY_LIST).build();
        testIllegalArgument(emptyKeyFields, String.format(ProfileGroupConfig.EMPTY_KEY_FIELDS_ERROR, profileGroupName, "empty"));
    }

    @Test
    public void testEmptyMeasurements() {
        String profileGroupName = "testMeasurements";

        ProfileGroupConfig nullMeasurements = ProfileGroupConfig.builder().profileGroupName(profileGroupName).
                sources(TEST_SOURCES).keyFieldNames(TEST_KEY_FIELDS).
                periodDuration(5L).periodDurationUnit(TimeUnit.MINUTES.name()).build();
        testNullField(nullMeasurements, String.format(ProfileGroupConfig.NULL_EMPTY_MEASUREMENTS_ERROR, profileGroupName, "null"));

        ProfileGroupConfig emptyMeasurements = ProfileGroupConfig.builder().profileGroupName(profileGroupName).
                sources(TEST_SOURCES).keyFieldNames(TEST_KEY_FIELDS).
                periodDuration(5L).periodDurationUnit(TimeUnit.MINUTES.name()).
                measurements(Lists.newArrayList()).build();
        testIllegalArgument(emptyMeasurements, String.format(ProfileGroupConfig.NULL_EMPTY_MEASUREMENTS_ERROR, profileGroupName, "empty"));
    }

    @Test
    public void testIllegalPeriodDuration() {
        String profileGroupName = "testPeriodDuration";

        // missing period
        ProfileGroupConfig nullPeriodDuration = ProfileGroupConfig.builder().profileGroupName(profileGroupName).
                sources(TEST_SOURCES).keyFieldNames(TEST_KEY_FIELDS).measurements(MEASUREMENTS).
                periodDurationUnit(TimeUnit.MINUTES.name()).build();
        testNullField(nullPeriodDuration, String.format(ProfileGroupConfig.PROFILE_TIME_ERROR, profileGroupName, "periodDuration", "null"));

        // missing period units
        ProfileGroupConfig nullPeriodDurationUnit = ProfileGroupConfig.builder().profileGroupName(profileGroupName).
                sources(TEST_SOURCES).keyFieldNames(TEST_KEY_FIELDS).measurements(MEASUREMENTS).
                periodDuration(5L).build();
        testNullField(nullPeriodDurationUnit, String.format(ProfileGroupConfig.PROFILE_TIME_ERROR, profileGroupName, "periodDurationUnit", "null"));

        // illegal period units
        ProfileGroupConfig illegalUnits = ProfileGroupConfig.builder().profileGroupName(profileGroupName).
                sources(TEST_SOURCES).keyFieldNames(TEST_KEY_FIELDS).measurements(MEASUREMENTS).
                periodDuration(5L).periodDurationUnit("illegal units").build();
        testIllegalField(illegalUnits, String.format(ProfileGroupConfig.PROFILE_TIME_ERROR, profileGroupName, "periodDurationUnit", "not a legal time unit"));

        // negative period duration
        ProfileGroupConfig negativePeriodDuration = ProfileGroupConfig.builder().profileGroupName(profileGroupName).
                sources(TEST_SOURCES).keyFieldNames(TEST_KEY_FIELDS).measurements(MEASUREMENTS).
                periodDuration(-5L).periodDurationUnit(TimeUnit.MINUTES.name()).build();
        testIllegalField(negativePeriodDuration, String.format(ProfileGroupConfig.PROFILE_TIME_ERROR, profileGroupName, "periodDuration", "0 or negative"));
    }

    @Test
    public void testIllegalStatsSlide() {
        String profileGroupName = "testStatsSlide";

        // missing stats slide
        ProfileGroupConfig nullStatsSlide = ProfileGroupConfig.builder().profileGroupName(profileGroupName).
                sources(TEST_SOURCES).keyFieldNames(TEST_KEY_FIELDS). measurements(MEASUREMENTS_WITH_STATS).
                periodDuration(10L).periodDurationUnit(TimeUnit.MINUTES.name()).
                statsSlideUnit(TimeUnit.SECONDS.name()).build();
        testNullField(nullStatsSlide, String.format(ProfileGroupConfig.PROFILE_TIME_ERROR, profileGroupName, "statsSlide", "null"));

        // missing stats slide units
        ProfileGroupConfig nullStatsSlideUnit = ProfileGroupConfig.builder().profileGroupName(profileGroupName).
                sources(TEST_SOURCES).keyFieldNames(TEST_KEY_FIELDS).measurements(MEASUREMENTS_WITH_STATS).
                periodDuration(10L).periodDurationUnit(TimeUnit.MINUTES.name()).
                statsSlide(5L).build();
        testNullField(nullStatsSlideUnit, String.format(ProfileGroupConfig.PROFILE_TIME_ERROR, profileGroupName, "statsSlideUnit", "null"));

        // illegal stats slide unit
        ProfileGroupConfig illegalUnits = ProfileGroupConfig.builder().profileGroupName(profileGroupName).
                sources(TEST_SOURCES).keyFieldNames(TEST_KEY_FIELDS).measurements(MEASUREMENTS_WITH_STATS).
                periodDuration(10L).periodDurationUnit(TimeUnit.MINUTES.name()).
                statsSlide(5L).statsSlideUnit("illegal unit").build();
        testIllegalField(illegalUnits, String.format(ProfileGroupConfig.PROFILE_TIME_ERROR, profileGroupName, "statsSlideUnit", "not a legal time unit"));

        // negative stats slide
        ProfileGroupConfig negativeStatsSlide = ProfileGroupConfig.builder().profileGroupName(profileGroupName).
                sources(TEST_SOURCES).keyFieldNames(TEST_KEY_FIELDS).measurements(MEASUREMENTS_WITH_STATS).
                periodDuration(10L).periodDurationUnit(TimeUnit.MINUTES.name()).
                statsSlide(-5L).statsSlideUnit(TimeUnit.MINUTES.name()).build();
        testIllegalField(negativeStatsSlide, String.format(ProfileGroupConfig.PROFILE_TIME_ERROR, profileGroupName, "statsSlide", "0 or negative"));

        // stats slide with no measurements that have stats
        ProfileGroupConfig slideWithoutStats = ProfileGroupConfig.builder().profileGroupName(profileGroupName).
                sources(TEST_SOURCES).keyFieldNames(TEST_KEY_FIELDS). measurements(MEASUREMENTS).
                periodDuration(10L).periodDurationUnit(TimeUnit.MINUTES.name()).
                statsSlide(5L).statsSlideUnit(TimeUnit.SECONDS.name()).build();
        testIllegalField(slideWithoutStats, String.format(ProfileGroupConfig.UNNECESSARY_STATS_SLIDE_ERROR, profileGroupName));
    }

    private void testNullField(ProfileGroupConfig badConfig, String expectedMessage) {
        assertThatThrownBy(badConfig::verify)
                .isInstanceOf(NullPointerException.class)
                .hasMessage(expectedMessage);
    }

    private void testIllegalField(ProfileGroupConfig badConfig, String expectedMessage) {
        assertThatThrownBy(badConfig::verify)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(expectedMessage);
    }

    private void testIllegalArgument(ProfileGroupConfig badConfig, String expectedMessage) {
        assertThatThrownBy(badConfig::verify)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
    }
}
