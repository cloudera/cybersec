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

import com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageConfig;
import com.cloudera.cyber.hbase.AbstractHbaseSinkFunction;
import org.apache.flink.api.java.utils.ParameterTool;


public class FirstSeenHbaseSink extends AbstractHbaseSinkFunction<ProfileMessage> {

    public FirstSeenHbaseSink(EnrichmentStorageConfig enrichmentStorageConfig, ProfileGroupConfig profileGroupConfig, ParameterTool params) {
        super(enrichmentStorageConfig.getHbaseTableName(), new FirstSeenHbaseMutationConverter(enrichmentStorageConfig, profileGroupConfig), params, "numFirstSeen");
    }

}
