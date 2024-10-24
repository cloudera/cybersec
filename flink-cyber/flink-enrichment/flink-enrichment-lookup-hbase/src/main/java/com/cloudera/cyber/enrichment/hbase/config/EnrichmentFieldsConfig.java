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

package com.cloudera.cyber.enrichment.hbase.config;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections.CollectionUtils;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnrichmentFieldsConfig implements Serializable {
    public static final String FIELD_CONFIG_INVALID_KEY_FIELD =
          "EnrichmentFieldsConfig %s: EnrichmentConfig.keyFields is null or empty.  Must contain at least one field name";
    public static final String RESERVED_ENRICH_DEFINES_KEY_FIELD =
          "EnrichmentFieldsConfig %s: EnrichmentConfig.keyFields for reserved enrichment type should be empty or null";
    public static final String RESERVED_ENRICH_DEFINES_VALUE_FIELD =
          "EnrichmentFieldsConfig %s: EnrichmentConfig.valueFields for reserved enrichment type should be empty or null";
    public static final String RESERVED_ENRICH_DEFINES_DELIMITER =
          "EnrichmentFieldsConfig %s: EnrichmentConfig.delimiter for reserved enrichment type should be null";
    public static final String FIELD_CONFIG_DUPLICATE_KEY_FIELD =
          "EnrichmentFieldsConfig %s: EnrichmentConfig.keyFields has duplicate values.  All key field names must be unique.";
    public static final String FIELD_CONFIG_INVALID_VALUE_FIELD =
          "EnrichmentFieldsConfig %s:EnrichmentConfig.valueFields is empty.  Must be null or list of fields";
    public static final String FIELD_CONFIG_DUPLICATE_VALUE_FIELD =
          "EnrichmentFieldsConfig %s: EnrichmentConfig.valueFields has duplicate values.  All value field names must be unique.";
    public static final String DEFAULT_KEY_DELIMITER = ":";
    public static final String THREATQ_ENRICHMENT_NAME = "threatq";
    public static final String FIRST_SEEN_ENRICHMENT_NAME = "first_seen";
    private static final Set<String> RESERVED_ENRICHMENT_NAMES =
          Stream.of(THREATQ_ENRICHMENT_NAME, FIRST_SEEN_ENRICHMENT_NAME).collect(Collectors.toSet());
    private static final ArrayList<String> EMPTY_LIST = new ArrayList<>();

    /**
     * Ordered list of field names to append together to create a key.  This key will be used to retrieve the
     * enrichment value from an existing field or combination of fields in the streaming event.
     */
    private ArrayList<String> keyFields;

    /**
     * The text used to separate the keys in the field.
     */
    private String keyDelimiter;

    /**
     * The name value pairs to associated with they.  These fields will be added to the event when an enrichment
     * is applied to an event and there is a matching key.
     */
    private ArrayList<String> valueFields;

    /**
     * List of event sources that can be used to write the enrichment type.
     */
    private ArrayList<String> streamingSources;

    public String getKeyDelimiter() {
        if (keyDelimiter == null) {
            return DEFAULT_KEY_DELIMITER;
        } else {
            return keyDelimiter;
        }
    }

    public ArrayList<String> getStreamingSources() {
        if (streamingSources == null) {
            return EMPTY_LIST;
        } else {
            return streamingSources;
        }
    }

    public void validate(String enrichmentType) {
        if (RESERVED_ENRICHMENT_NAMES.contains(enrichmentType)) {
            Preconditions.checkState(CollectionUtils.isEmpty(keyFields),
                  String.format(RESERVED_ENRICH_DEFINES_KEY_FIELD, enrichmentType));
            Preconditions.checkState(CollectionUtils.isEmpty(valueFields),
                  String.format(RESERVED_ENRICH_DEFINES_VALUE_FIELD, enrichmentType));
            Preconditions.checkState(keyDelimiter == null,
                  String.format(RESERVED_ENRICH_DEFINES_DELIMITER, enrichmentType));
        } else {
            Preconditions.checkState(CollectionUtils.isNotEmpty(keyFields),
                  String.format(FIELD_CONFIG_INVALID_KEY_FIELD, enrichmentType));
            Preconditions.checkState(isUnique(keyFields),
                  String.format(FIELD_CONFIG_DUPLICATE_KEY_FIELD, enrichmentType));
            Preconditions.checkState(valueFields == null || !valueFields.isEmpty(),
                  String.format(FIELD_CONFIG_INVALID_VALUE_FIELD, enrichmentType));
            Preconditions.checkState(valueFields == null || isUnique(valueFields),
                  String.format(FIELD_CONFIG_DUPLICATE_VALUE_FIELD, enrichmentType));
        }
    }

    private static boolean isUnique(List<String> listToCheck) {
        return listToCheck.stream().distinct().count() == listToCheck.size();
    }
}
