package com.cloudera.cyber.enrichment.stix;

import com.cloudera.cyber.flink.UUIDUtils;
import org.apache.flink.addons.hbase.HBaseSinkFunction;
import org.apache.hadoop.hbase.client.BufferedMutator;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

public class ThreatIntelligenceDetailsHBaseSinkFunction extends HBaseSinkFunction<ThreatIntelligenceDetails> {

    private static final byte[] cf = Bytes.toBytes("d");
    private static final byte[] q = Bytes.toBytes("stix");

    public ThreatIntelligenceDetailsHBaseSinkFunction(String hTableName) {
        super(hTableName);
    }

    @Override
    public void executeMutations(ThreatIntelligenceDetails threatIntelligenceDetails, Context context, BufferedMutator bufferedMutator) throws Exception {
        Put put = new Put(UUIDUtils.asBytes(threatIntelligenceDetails.getId()));
        put.addColumn(cf, q, Bytes.toBytes(threatIntelligenceDetails.getStixSource()));
        bufferedMutator.mutate(put);
    }
}
