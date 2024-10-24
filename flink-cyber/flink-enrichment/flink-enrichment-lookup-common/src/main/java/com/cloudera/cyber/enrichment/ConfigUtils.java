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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;

import com.cloudera.cyber.enrichment.lookup.config.EnrichmentConfig;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentField;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentKind;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConfigUtils {
    public static final String PARAMS_CONFIG_FILE = "config.file";

    public static Map<String, List<String>> typeToFields(List<EnrichmentConfig> allConfigs, EnrichmentKind kind) {
        return allConfigs.stream()
                         .filter(c -> c.getKind() == kind)
                         .flatMap(c -> c.getFields().stream()
                                        .collect(groupingBy(EnrichmentField::getEnrichmentType,
                                              mapping(EnrichmentField::getName, Collectors.toList())))
                                        .entrySet().stream()
                         )
                         .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) ->
                               Stream.of(a, b)
                                     .flatMap(Collection::stream)
                                     .collect(Collectors.toList())
                         ));
    }

    public static Set<String> enrichmentTypes(List<EnrichmentConfig> allConfigs, EnrichmentKind kind) {
        return allConfigs.stream()
                         .filter(f -> f.getKind() == kind)
                         .flatMap(s -> s.getFields().stream().map(EnrichmentField::getEnrichmentType))
                         .collect(Collectors.toSet());
    }

    public static List<EnrichmentConfig> allConfigs(byte[] configJson) throws IOException {
        return new ObjectMapper().readValue(
              configJson,
              new TypeReference<List<EnrichmentConfig>>() {
              });
    }
}
