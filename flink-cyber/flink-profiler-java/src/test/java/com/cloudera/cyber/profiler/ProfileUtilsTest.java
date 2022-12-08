package com.cloudera.cyber.profiler;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.cloudera.cyber.profiler.dto.MeasurementDto;
import com.cloudera.cyber.profiler.dto.ProfileDto;
import org.junit.Test;

public class ProfileUtilsTest {

    @Test
    public void compareProfilesWithoutMeasurement() {
        ProfileDto profile1 = ProfileDto.builder()
            .id(1)
            .profileGroupName("profile1")
            .keyFieldNames("test")
            .statsSlide(1L)
            .statsSlideUnit("MINUTES")
            .periodDuration(1L)
            .periodDurationUnit("MINUTES")
            .build();

        ProfileDto newProfile1 = ProfileDto.builder()
            .profileGroupName("profile1")
            .keyFieldNames("test")
            .statsSlide(60L)
            .statsSlideUnit("SECONDS")
            .periodDuration(60L)
            .periodDurationUnit("SECONDS")
            .build();

        ProfileDto profile2 = ProfileDto.builder()
            .id(2)
            .profileGroupName("profile2")
            .keyFieldNames("test")
            .statsSlide(1L)
            .statsSlideUnit("MINUTES")
            .periodDuration(1L)
            .periodDurationUnit("MINUTES")
            .build();

        ProfileDto newIncorrectProfile1 = ProfileDto.builder()
            .id(2)
            .profileGroupName("profile1")
            .keyFieldNames("test")
            .statsSlide(10L)
            .statsSlideUnit("MINUTES")
            .periodDuration(10L)
            .periodDurationUnit("MINUTES")
            .build();

        assertThat(ProfileUtils.compareProfileDto(profile1, newProfile1)).isTrue();
        assertThat(ProfileUtils.compareProfileDto(profile1, profile2)).isFalse();
        assertThat(ProfileUtils.compareProfileDto(profile1, newIncorrectProfile1)).isFalse();
    }

    @Test
    public void measurementCompare() {
        MeasurementDto measurement1 = MeasurementDto.builder()
            .id(1)
            .profileId(1)
            .format("format")
            .fieldName("test")
            .resultExtensionName("resTest")
            .calculateStats(true)
            .firstSeenExpirationDuration(60L)
            .firstSeenExpirationDurationUnit("SECONDS")
            .build();

        MeasurementDto measurement2 = MeasurementDto.builder()
            .id(2)
            .profileId(1)
            .format("format2")
            .fieldName("test2")
            .resultExtensionName("resTest2")
            .calculateStats(false)
            .firstSeenExpirationDuration(60L)
            .firstSeenExpirationDurationUnit("MINUTES")
            .build();

        MeasurementDto measurement1Updated = MeasurementDto.builder()
            .id(1)
            .profileId(1)
            .format("format")
            .fieldName("test")
            .resultExtensionName("resTest")
            .calculateStats(false)
            .firstSeenExpirationDuration(1L)
            .firstSeenExpirationDurationUnit("MINUTES")
            .build();

        MeasurementDto measurement2Updated = MeasurementDto.builder()
            .id(2)
            .profileId(1)
            .format("format2")
            .fieldName("test2")
            .resultExtensionName("resTest2")
            .calculateStats(true)
            .firstSeenExpirationDuration(1L)
            .firstSeenExpirationDurationUnit("HOURS")
            .build();

        MeasurementDto measurement1Corrupted = MeasurementDto.builder()
            .id(1)
            .profileId(1)
            .format("corupted")
            .fieldName("corupted")
            .resultExtensionName("resTest")
            .calculateStats(false)
            .firstSeenExpirationDuration(1L)
            .firstSeenExpirationDurationUnit("MINUTES")
            .build();

        MeasurementDto measurement2Corrupted = MeasurementDto.builder()
            .id(2)
            .profileId(1)
            .format("format2")
            .fieldName("test2")
            .resultExtensionName("resTest2")
            .calculateStats(true)
            .firstSeenExpirationDuration(2L)
            .firstSeenExpirationDurationUnit("HOURS")
            .build();

        assertThat(ProfileUtils.compareMeasurementDto(measurement1, measurement1Updated)).isTrue();
        assertThat(ProfileUtils.compareMeasurementDto(measurement2, measurement2Updated)).isTrue();
        assertThat(ProfileUtils.compareMeasurementDto(measurement1, measurement1Corrupted)).isFalse();
        assertThat(ProfileUtils.compareMeasurementDto(measurement2, measurement2Corrupted)).isFalse();
        assertThat(ProfileUtils.compareMeasurementDto(measurement1, measurement2)).isFalse();
    }
}