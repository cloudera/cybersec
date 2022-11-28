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

package com.cloudera.cyber.enrichment.stix.parsing.types;

import com.cloudera.cyber.ThreatIntelligence;
import org.mitre.cybox.common_2.ObjectPropertiesType;

import java.util.function.Function;

/**
 *
 * Inspired by Metron: see https://github.com/apache/metron/tree/2ee6cc7e0b448d8d27f56f873e2c15a603c53917/metron-platform/metron-data-management/src/main/java/org/apache/metron/dataloads/extractor/stix
 * @param <T>
 */
public abstract class AbstractObjectTypeHandler<T extends ObjectPropertiesType> implements ObjectTypeHandler<T> {
    protected Class<T> objectPropertiesType;

    public AbstractObjectTypeHandler(Class<T> clazz) {
        objectPropertiesType = clazz;
    }

    @Override
    public Class<T> getTypeClass() {
        return objectPropertiesType;
    }

    public String getType() {
        return getTypeClass().getSimpleName();
    }

    protected Function<String, ThreatIntelligence.ThreatIntelligenceBuilder> mapToThreatIntelligence(String type) {
        return value -> ThreatIntelligence.builder()
                .observable(value)
                .observableType(type);
    }

}
