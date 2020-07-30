package com.cloudera.cyber.enrichment.stix.parsing.types;

import org.mitre.cybox.common_2.ObjectPropertiesType;

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
}
