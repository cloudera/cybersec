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

import com.google.common.base.Joiner;

import java.util.Map;

public class MetronGeoEnrichment extends Enrichment {
    public MetronGeoEnrichment(String fieldName, String feature) {
        super(fieldName, feature);
    }

    @Override
    protected String getName(String enrichmentName) {
        return Joiner.on(DELIMITER).skipNulls().join(this.fieldName, enrichmentName);
    }

    @Override
    public void enrich(Map<String, String> extensions, String enrichmentName, Object enrichmentValue) {
        if (enrichmentValue != null) {
            String extensionName =getName(enrichmentName);
            extensions.put(extensionName, enrichmentValue.toString());
        }

    }
}
