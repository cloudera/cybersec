package com.cloudera.cyber.enrichment.stix;

import com.cloudera.cyber.ThreatIntelligence;
import com.cloudera.cyber.flink.UUIDUtils;
import org.apache.flink.addons.hbase.HBaseSinkFunction;
import org.apache.hadoop.hbase.client.BufferedMutator;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ThreatIntelligenceHBaseSinkFunction extends HBaseSinkFunction<ThreatIntelligence> {

    private static final byte[] cf = Bytes.toBytes("t");
    private static final byte[] indicator = Bytes.toBytes("i");
    private static final byte[] indicatorType = Bytes.toBytes("it");

    public ThreatIntelligenceHBaseSinkFunction(String hTableName) {
        super(hTableName);
    }

    @Override
    public void executeMutations(ThreatIntelligence threatIntelligence, Context context, BufferedMutator bufferedMutator) throws Exception {
        Put put = new Put(UUIDUtils.asBytes(UUID.fromString(threatIntelligence.getId())));

        put.addColumn(cf, indicator, threatIntelligence.getObservable().getBytes(StandardCharsets.UTF_8));
        put.addColumn(cf, indicatorType, threatIntelligence.getObservableType().getBytes(StandardCharsets.UTF_8));

        // add all the other columns

        bufferedMutator.mutate(put);
    }

}
