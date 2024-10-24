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

import com.cloudera.cyber.profiler.accumulator.FieldValueProfileGroupAcc;
import com.cloudera.cyber.profiler.accumulator.ProfileGroupAcc;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.stream.Collectors;

public class FieldValueProfileAggregateFunction extends ProfileAggregateFunction {

    public FieldValueProfileAggregateFunction(ProfileGroupConfig profileGroupConfig) {
        super(profileGroupConfig, profileGroupConfig.getProfileGroupName());
    }

    @Override
    public ProfileGroupAcc createAccumulator() {
        return new FieldValueProfileGroupAcc(profileGroupConfig);
    }

    @Override
    protected Map<String, DecimalFormat> getMeasurementFormats() {
        return profileGroupConfig.getMeasurements().stream()
                                 .collect(Collectors.toMap(ProfileMeasurementConfig::getResultExtensionName,
                                       ProfileMeasurementConfig::getDecimalFormat));
    }
}
