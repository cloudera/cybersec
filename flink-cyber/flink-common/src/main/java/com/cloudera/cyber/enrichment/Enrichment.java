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

package com.cloudera.cyber.enrichment;


import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.DataQualityMessageLevel;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class Enrichment {

    public static final String DELIMITER = ".";

    protected final String fieldName;
    protected final String feature;
    protected final String prefix;


    public Enrichment(String fieldName, String feature) {
        this.fieldName = fieldName;
        this.feature = feature;
        this.prefix = String.join(DELIMITER, fieldName, feature);
    }

    protected String getName(String enrichmentName) {
        return String.join(DELIMITER, prefix, enrichmentName);
    }

    public List<DataQualityMessage> addQualityMessage(List<DataQualityMessage> messages, DataQualityMessageLevel level,
                                                      String message) {
        Optional<DataQualityMessage> duplicate = messages.stream()
              .filter(m -> m.getLevel().equals(level.name()) && m.getField().equals(fieldName)
                           && m.getFeature().equals(feature) && m.getMessage().equals(message)).findFirst();
        if (!duplicate.isPresent()) {
            messages.add(DataQualityMessage.builder()
                  .level(level.name())
                  .feature(feature)
                  .field(fieldName)
                  .message(message).build());
        }
        return messages;
    }

    public abstract void enrich(Map<String, String> extensions, String enrichmentName, Object enrichmentValue);
}