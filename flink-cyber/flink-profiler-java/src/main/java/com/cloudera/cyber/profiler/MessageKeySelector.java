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

import com.cloudera.cyber.scoring.ScoredMessage;
import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.flink.api.java.functions.KeySelector;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class MessageKeySelector implements KeySelector<ProfileMessage, String>{

    private List<String> fieldNames;

    public MessageKeySelector(List<String> fieldNames) {
        Preconditions.checkNotNull(fieldNames, "profile key field list is null");
        Preconditions.checkArgument(!fieldNames.isEmpty(),"profiled key field list must contain at least one field name");
        this.fieldNames = fieldNames;
    }

    @Override
    public String getKey(ProfileMessage message) {
        Map<String, String> extensions = message.getExtensions();
        return fieldNames.stream().map(extensions::get).collect(Collectors.joining("-"));
    }
}