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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.flink.api.common.functions.MapFunction;

public class ScoredMessageToProfileMessageMap implements MapFunction<ScoredMessage, ProfileMessage> {

    public static final String CYBER_SCORE_FIELD = "cyberscore";

    private final List<String> allFieldNames;

    public ScoredMessageToProfileMessageMap(ProfileGroupConfig profileGroupConfig) {
        this.allFieldNames = profileGroupConfig.getMeasurementFieldNames();
        this.allFieldNames.addAll(profileGroupConfig.getKeyFieldNames());
    }

    @Override
    public ProfileMessage map(ScoredMessage scoredMessage) {
        Map<String, String> reducedExtensions = new HashMap<>();
        Map<String, String> allExtensions = scoredMessage.getMessage().getExtensions();
        allFieldNames.forEach(fieldName -> {
            {
                if (CYBER_SCORE_FIELD.equals(fieldName)) {
                    reducedExtensions.put(CYBER_SCORE_FIELD, scoredMessage.getCyberScore().toString());
                } else {
                    String extensionValue = allExtensions.get(fieldName);
                    if (extensionValue != null) {
                        reducedExtensions.put(fieldName, extensionValue);
                    }
                }
            }
        });

        return new ProfileMessage(scoredMessage.getTs(), reducedExtensions);
    }
}
