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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EnrichmentStorageConfig implements Serializable {
    public static final String STORAGE_CONFIG_TABLE_NOT_SET_ERROR = "EnrichmentStorageConfig %s: Hbase table name must be set and non-empty";
    public static final String STORAGE_CONFIG_COLUMN_FAMILY_NOT_SET_ERROR = "EnrichmentStorageConfig %s: Hbase metron enrichment format requires column family";
    public static final String STORAGE_CONFIG_COLUMN_FAMILY_SET_ERROR = "EnrichmentStorageConfig %s: Hbase simple enrichment format does not require a column family";

    private EnrichmentStorageFormat format;
    private String hbaseTableName;
    private String columnFamily;

    public void validate(String storageType) {
        Preconditions.checkState(StringUtils.isNotEmpty(hbaseTableName), STORAGE_CONFIG_TABLE_NOT_SET_ERROR, storageType);
        Preconditions.checkState(format.equals(EnrichmentStorageFormat.HBASE_SIMPLE) || StringUtils.isNotEmpty(columnFamily), String.format(STORAGE_CONFIG_COLUMN_FAMILY_NOT_SET_ERROR, storageType));
        Preconditions.checkState(format.equals(EnrichmentStorageFormat.HBASE_METRON) || StringUtils.isEmpty(columnFamily), String.format
            (STORAGE_CONFIG_COLUMN_FAMILY_SET_ERROR, storageType));
    }
}
