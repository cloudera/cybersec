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

import com.cloudera.cyber.profiler.accumulator.StatsProfileGroupAcc;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.flink.api.common.functions.JoinFunction;

public class ProfileStatsJoin implements JoinFunction<ProfileMessage, ProfileMessage, ProfileMessage> {

    public static boolean isStatsExtension(String extensionName) {
        int suffixStart = extensionName.lastIndexOf('.');
        if (suffixStart > 0) {
            return StatsProfileGroupAcc.STATS_EXTENSION_SUFFIXES.contains(extensionName.substring(suffixStart));
        }
        return false;
    }

    @Override
    public ProfileMessage join(ProfileMessage profileMessage, ProfileMessage statsMessage) {
        Map<String, String> statsExtensions =
              statsMessage.getExtensions().entrySet().stream().filter(e -> isStatsExtension(e.getKey()))
                          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<String, String> extensionsWithStats = new HashMap<>(profileMessage.getExtensions());
        extensionsWithStats.putAll(statsExtensions);
        return new ProfileMessage(profileMessage.getTs(), extensionsWithStats);
    }
}