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

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.java.functions.KeySelector;

public class StatsProfileKeySelector implements KeySelector<ProfileMessage, String> {

    @Override
    public String getKey(ProfileMessage message) {
        String statsProfileName = message.getExtensions()
                                         .getOrDefault(ProfileAggregateFunction.PROFILE_GROUP_NAME_EXTENSION,
                                               StatsProfileAggregateFunction.STATS_PROFILE_GROUP_SUFFIX);
        return StringUtils.removeEnd(statsProfileName, StatsProfileAggregateFunction.STATS_PROFILE_GROUP_SUFFIX);
    }
}
