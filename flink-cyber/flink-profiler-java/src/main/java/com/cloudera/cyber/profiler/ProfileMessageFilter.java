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

import com.cloudera.cyber.Message;
import com.cloudera.cyber.scoring.ScoredMessage;
import org.apache.flink.api.common.functions.FilterFunction;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Filters messages that can contribute to an aggregation of a profile.
 *
 * The message source must match one of the messages in the source list or if there are no sources specified
 * don't filter based on source.
 *
 * If the profile contains only measurements that need field values, filter the message if it doesn't contain one of these fields.
 * Otherwise, if one of the profile measurement is cyber score details or does not need a field, return the message.  The cyber score details
 * field is always guaranteed to be specified.
 */
public class ProfileMessageFilter implements FilterFunction<ScoredMessage> {

    private final List<String> sources;
    private final List<String> keyFieldNames;
    private final List<String> measurementFieldNames;

    public ProfileMessageFilter(ProfileGroupConfig profileGroupConfig) {
        if (profileGroupConfig.needsSourceFilter()) {
            this.sources = profileGroupConfig.getSources();
        } else {
            this.sources = null;
        }
        this.keyFieldNames = profileGroupConfig.getKeyFieldNames().stream().filter(fieldName -> !ScoredMessageToProfileMessageMap.CYBER_SCORE_FIELD.equals(fieldName)).collect(Collectors.toList());
        List<String> optionalFields = profileGroupConfig.getMeasurementFieldNames().stream().
                filter(fieldName -> !ScoredMessageToProfileMessageMap.CYBER_SCORE_FIELD.equals(fieldName)).
                collect(Collectors.toList());
        if (optionalFields.size() == profileGroupConfig.getMeasurements().size()) {
            this.measurementFieldNames = optionalFields;
        } else {
            this.measurementFieldNames = null;
        }
    }

    @Override
    public boolean filter(ScoredMessage scoredMessage) {
        Message message = scoredMessage.getMessage();
        Map<String, String> extensions = scoredMessage.getMessage().getExtensions();
        return ((sources == null || sources.contains(message.getSource())) &&
                extensions != null &&
                extensions.keySet().containsAll(keyFieldNames) &&
                (measurementFieldNames == null || measurementFieldNames.stream().anyMatch(extensions::containsKey)));
    }
}
