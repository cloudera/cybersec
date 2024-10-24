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

import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentConfig;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentField;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentKind;
import java.util.ArrayList;
import java.util.List;
import org.apache.flink.api.common.functions.FilterFunction;

public class FilterEnrichmentType implements FilterFunction<EnrichmentCommand> {
    private final List<String> enrichmentTypes = new ArrayList<>();

    public FilterEnrichmentType(List<EnrichmentConfig> enrichmentConfigStream, EnrichmentKind kind) {
        for (EnrichmentConfig enrichmentConfig : enrichmentConfigStream) {
            if (enrichmentConfig.getKind().equals(kind)) {
                for (EnrichmentField field : enrichmentConfig.getFields()) {
                    enrichmentTypes.add(field.getEnrichmentType());
                }
            }
        }
    }

    @Override
    public boolean filter(EnrichmentCommand enrichmentCommand) {
        return enrichmentTypes.contains(enrichmentCommand.getPayload().getType());
    }
}
