package com.cloudera.cyber.enrichment.hbase.config;

import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections.CollectionUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnrichmentFieldsConfig implements Serializable {
    public static final String FIELD_CONFIG_INVALID_KEY_FIELD = "EnrichmentFieldsConfig %s: EnrichmentConfig.keyFields is null or empty.  Must contain at least one field name";
    public static final String FIELD_CONFIG_DUPLICATE_KEY_FIELD = "EnrichmentFieldsConfig %s: EnrichmentConfig.keyFields has duplicate values.  All key field names must be unique.";
    public static final String FIELD_CONFIG_INVALID_VALUE_FIELD = "EnrichmentFieldsConfig %s:EnrichmentConfig.valueFields is empty.  Must be null or list of fields";
    public static final String FIELD_CONFIG_DUPLICATE_VALUE_FIELD = "EnrichmentFieldsConfig %s: EnrichmentConfig.valueFields has duplicate values.  All value field names must be unique.";
    public static final String DEFAULT_KEY_DELIMITER = ":";
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
        Preconditions.checkState(CollectionUtils.isNotEmpty(keyFields), String.format(FIELD_CONFIG_INVALID_KEY_FIELD, enrichmentType) );
        Preconditions.checkState(isUnique(keyFields), String.format(FIELD_CONFIG_DUPLICATE_KEY_FIELD, enrichmentType));
        Preconditions.checkState(valueFields == null || !valueFields.isEmpty(), String.format(FIELD_CONFIG_INVALID_VALUE_FIELD, enrichmentType));
        Preconditions.checkState(valueFields == null || isUnique(valueFields), String.format(FIELD_CONFIG_DUPLICATE_VALUE_FIELD, enrichmentType));
    }

    private static boolean isUnique(List<String> listToCheck) {
        return listToCheck.stream().distinct().count() == listToCheck.size();
    }
}
