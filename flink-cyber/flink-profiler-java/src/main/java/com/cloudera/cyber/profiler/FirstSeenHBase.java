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

import com.cloudera.cyber.hbase.LookupKey;
import com.cloudera.cyber.profiler.accumulator.ProfileGroupAcc;
import lombok.Data;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
public class FirstSeenHBase implements Serializable {
    private static final long serialVersionUID = 1L;

    private String tableName;
    private byte[] columnFamilyName;
    private String profileName;
    private List<String> keyFieldNames;
    private String firstSeenResultName;

    public FirstSeenHBase(String tableName, String columnFamilyName, ProfileGroupConfig profileGroupConfig) {
        this.tableName = tableName;
        this.columnFamilyName = Bytes.toBytes(columnFamilyName);
        this.profileName = profileGroupConfig.getProfileGroupName();
        this.keyFieldNames = profileGroupConfig.getKeyFieldNames();
        ProfileMeasurementConfig measurementConfig = profileGroupConfig.getMeasurements().stream().filter(m -> m.getAggregationMethod().equals(ProfileAggregationMethod.FIRST_SEEN)).
                findFirst().orElseThrow(() -> new NullPointerException("Expected at least one first seen measurement but none was found."));
        this.firstSeenResultName = measurementConfig.getResultExtensionName();
    }

    public LookupKey getKey(ProfileMessage message) {
        Map<String, String> extensions = message.getExtensions();
        String key = Stream.concat(Stream.of(profileName),
                keyFieldNames.stream().map(extensions::get)).collect(Collectors.joining(":"));
        return LookupKey.builder()
                .key(Bytes.toBytes(key))
                .cf(columnFamilyName)
                .build();
    }

    public String getFirstSeen(ProfileMessage message) {
        return message.getExtensions().get(ProfileGroupAcc.START_PERIOD_EXTENSION);
    }

    public String getLastSeen(ProfileMessage message) {
        return message.getExtensions().get(ProfileGroupAcc.END_PERIOD_EXTENSION);
    }

}
