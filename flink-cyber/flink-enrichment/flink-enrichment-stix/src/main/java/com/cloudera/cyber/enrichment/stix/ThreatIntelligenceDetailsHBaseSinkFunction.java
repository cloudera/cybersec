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

import com.cloudera.cyber.enrichment.stix.parsing.ThreatIntelligenceDetails;
import com.cloudera.cyber.flink.UUIDUtils;
import com.cloudera.cyber.hbase.AbstractHbaseSinkFunction;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.hbase.sink.HBaseMutationConverter;
import org.apache.hadoop.hbase.client.BufferedMutator;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

import java.util.UUID;

public class ThreatIntelligenceDetailsHBaseSinkFunction extends AbstractHbaseSinkFunction<ThreatIntelligenceDetails> {

    private static final byte[] cf = Bytes.toBytes("d");
    private static final byte[] q = Bytes.toBytes("stix");

    private static final HBaseMutationConverter<ThreatIntelligenceDetails> THREAT_INTELLIGENCE_DETAILS_MUTATION_CONVERTER = new HBaseMutationConverter<ThreatIntelligenceDetails>() {
        @Override
        public void open() {

        }

        @Override
        public Mutation convertToMutation(ThreatIntelligenceDetails threatIntelligenceDetails) {
            Put put = new Put(UUIDUtils.asBytes(UUID.fromString(threatIntelligenceDetails.getId())));
            put.addColumn(cf, q, Bytes.toBytes(threatIntelligenceDetails.getStixSource()));
            return put;
        }
    };

    public ThreatIntelligenceDetailsHBaseSinkFunction(String hTableName, ParameterTool params) {
        super(hTableName, THREAT_INTELLIGENCE_DETAILS_MUTATION_CONVERTER, params, "numThreatIntelDetails");
    }
}
