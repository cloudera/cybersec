package com.cloudera.cyber.profiler;

import java.util.Collections;
import java.util.List;

public class ProfileAggregateTest extends ProfileGroupTest {

    protected ProfileGroupConfig getProfileGroupConfig(boolean calculateStats) {
        List<ProfileMeasurementConfig> measurements = Collections.singletonList(ProfileMeasurementConfig.builder().resultExtensionName(RESULT_EXTENSION_NAME).
                aggregationMethod(ProfileAggregationMethod.SUM).fieldName(SUM_FIELD_NAME).format("0.000000").calculateStats(calculateStats).
                build());

        return getProfileGroupConfig(PROFILE_GROUP_NAME, measurements);
    }

}
