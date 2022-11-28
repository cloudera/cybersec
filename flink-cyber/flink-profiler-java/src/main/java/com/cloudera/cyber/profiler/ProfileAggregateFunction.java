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

import com.cloudera.cyber.profiler.accumulator.ProfileGroupAcc;
import lombok.AllArgsConstructor;
import org.apache.flink.api.common.functions.AggregateFunction;

import java.text.DecimalFormat;
import java.util.Map;

@AllArgsConstructor
public abstract class ProfileAggregateFunction implements AggregateFunction<ProfileMessage, ProfileGroupAcc, ProfileMessage> {
    public static final String PROFILE_GROUP_NAME_EXTENSION = "profile";
    public static final String PROFILE_TOPIC_NAME = "profile";
    public static final String PROFILE_SOURCE = "profile";

    protected ProfileGroupConfig profileGroupConfig;
    private final String profileGroupName;
    private final Map<String, DecimalFormat> measurementFormats;

    public ProfileAggregateFunction(ProfileGroupConfig profileGroupConfig, String profileGroupName) {
       this.profileGroupConfig = profileGroupConfig;
       this.profileGroupName = profileGroupName;
       this.measurementFormats = getMeasurementFormats();
    }

    @Override
    public abstract ProfileGroupAcc createAccumulator();

    @Override
    public ProfileGroupAcc add(ProfileMessage message, ProfileGroupAcc profileAccumulator) {
        profileAccumulator.addMessage(message, profileGroupConfig);
        return profileAccumulator;
    }

    @Override
    public ProfileMessage getResult(ProfileGroupAcc profileAccumulator) {
        Map<String, String> extensions = profileAccumulator.getProfileExtensions(profileGroupConfig, measurementFormats);
        extensions.put(PROFILE_GROUP_NAME_EXTENSION, profileGroupName);
        return new ProfileMessage(profileAccumulator.getEndTimestamp(), extensions);
    }

    @Override
    public ProfileGroupAcc merge(ProfileGroupAcc acc1, ProfileGroupAcc acc2) {
         acc1.merge(acc2);
         return acc1;
    }

    protected abstract Map<String, DecimalFormat> getMeasurementFormats();

}
