package com.cloudera.cyber.enrichment.stix;

import com.cloudera.cyber.ThreatIntelligence;
import com.cloudera.cyber.flink.UUIDUtils;
import org.apache.flink.addons.hbase.HBaseSinkFunction;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.hadoop.hbase.client.BufferedMutator;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

import java.nio.charset.StandardCharsets;

public class ThreatIndexHbaseSinkFunction extends HBaseSinkFunction<ThreatIntelligence> {

    private static final byte[] cf = Bytes.toBytes("t");
    private static final byte[] id = Bytes.toBytes("id");
    private static final byte[] fields = Bytes.toBytes("f");

    public ThreatIndexHbaseSinkFunction(String hTableName) {
        super(hTableName);
    }

    @Override
    public void executeMutations(ThreatIntelligence threatIntelligence, SinkFunction.Context context, BufferedMutator bufferedMutator) throws Exception {
        // key by the indicator value - note that this should really be salted to avoid hotspots

        Put put = new Put(threatIntelligence.getObservable().getBytes(StandardCharsets.UTF_8));
        put.addColumn(cf, id, UUIDUtils.asBytes(threatIntelligence.getId()));

        // put in all the fields
        threatIntelligence.getFields().entrySet().stream().forEach(
                f -> put.addColumn(fields, Bytes.toBytes(f.getKey()), Bytes.toBytes(f.getValue()))
        );

        bufferedMutator.mutate(put);
    }
}
