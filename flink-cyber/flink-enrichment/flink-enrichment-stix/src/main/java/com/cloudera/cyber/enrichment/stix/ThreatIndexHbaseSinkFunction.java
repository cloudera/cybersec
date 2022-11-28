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

package com.cloudera.cyber.enrichment.stix;

import com.cloudera.cyber.ThreatIntelligence;
import com.cloudera.cyber.flink.UUIDUtils;
import com.cloudera.cyber.hbase.AbstractHbaseSinkFunction;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.hbase.sink.HBaseMutationConverter;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ThreatIndexHbaseSinkFunction extends AbstractHbaseSinkFunction<ThreatIntelligence> {
    private static final byte[] cf = Bytes.toBytes("t");
    private static final byte[] id = Bytes.toBytes("id");
    private static final byte[] fields = Bytes.toBytes("f");

    private static final HBaseMutationConverter<ThreatIntelligence> THREAT_INTEL_MUTATION_CONVERTER = new HBaseMutationConverter<ThreatIntelligence>() {
        @Override
        public void open() {

        }

        @Override
        public Mutation convertToMutation(ThreatIntelligence threatIntelligence) {
            // key by the indicator value - note that this should really be salted to avoid hotspots
            Put put = new Put((threatIntelligence.getObservableType() + ":" + threatIntelligence.getObservable()).getBytes(StandardCharsets.UTF_8));

            // TODO allow for multi-value
            put.addColumn(cf, id, UUIDUtils.asBytes(UUID.fromString(threatIntelligence.getId())));

            // put in all the fields
            threatIntelligence.getFields().forEach((key, value) -> put.addColumn(fields, Bytes.toBytes(key), Bytes.toBytes(value)));
            return put;
        }
    };

    public ThreatIndexHbaseSinkFunction(String hTableName, ParameterTool params) {
        super(hTableName, THREAT_INTEL_MUTATION_CONVERTER, params, "numThreatIndex");
    }

}
