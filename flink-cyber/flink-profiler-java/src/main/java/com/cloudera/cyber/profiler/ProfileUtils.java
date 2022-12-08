package com.cloudera.cyber.profiler;

import com.cloudera.cyber.flink.Utils;
import com.cloudera.cyber.profiler.dto.MeasurementDto;
import com.cloudera.cyber.profiler.dto.ProfileDto;
import java.util.Objects;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ProfileUtils {

    public static boolean compareProfileDto(ProfileDto profileDto1, ProfileDto profileDto2) {
        if (profileDto1 == null || profileDto2 == null) {
            return false;
        }
        if (profileDto1 == profileDto2) {
            return true;
        }

        return Objects.equals(profileDto1.getProfileGroupName(), profileDto2.getProfileGroupName()) && Objects.equals(profileDto1.getKeyFieldNames(), profileDto2.getKeyFieldNames())
            && Utils.timeCompare(profileDto1.getPeriodDuration(), profileDto1.getPeriodDurationUnit(), profileDto2.getPeriodDuration(), profileDto2.getPeriodDurationUnit())
            && Utils.timeCompare(profileDto1.getStatsSlide(), profileDto1.getStatsSlideUnit(),
            profileDto2.getStatsSlide(), profileDto2.getStatsSlideUnit());
    }


    public static boolean compareMeasurementDto(MeasurementDto measurementDto1, MeasurementDto measurementDto2) {
        if (measurementDto1 == null || measurementDto2 == null) {
            return false;
        }
        if (measurementDto1 == measurementDto2) {
            return true;
        }
        return Objects.equals(measurementDto1.getFieldName(), measurementDto2.getFieldName()) && Objects.equals(measurementDto1.getResultExtensionName(), measurementDto2.getResultExtensionName()) && Objects.equals(
            measurementDto1.getAggregationMethod(), measurementDto2.getAggregationMethod()) && Objects.equals(measurementDto1.getFormat(),
            measurementDto2.getFormat()) && Utils.timeCompare(measurementDto1.getFirstSeenExpirationDuration(), measurementDto1.getFirstSeenExpirationDurationUnit(), measurementDto2.getFirstSeenExpirationDuration(),
            measurementDto2.getFirstSeenExpirationDurationUnit());
    }

}
