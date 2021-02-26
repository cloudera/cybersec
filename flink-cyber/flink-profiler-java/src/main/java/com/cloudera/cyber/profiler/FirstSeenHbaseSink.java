package com.cloudera.cyber.profiler;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.enrichment.hbase.LookupKey;
import org.apache.flink.addons.hbase.HBaseSinkFunction;
import org.apache.hadoop.hbase.client.BufferedMutator;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

import static com.cloudera.cyber.enrichment.EnrichmentUtils.CF_ID;
import static com.cloudera.cyber.enrichment.EnrichmentUtils.Q_KEY;


public class FirstSeenHbaseSink extends HBaseSinkFunction<Message> {
    private final FirstSeenHBase firstSeenHBase;

    public FirstSeenHbaseSink(String hTableName, String columnFamily, ProfileGroupConfig profileGroupConfig) {
        super(hTableName);
        this.firstSeenHBase = new FirstSeenHBase(hTableName, columnFamily, profileGroupConfig);
    }

    @Override
    public void executeMutations(Message message, Context context, BufferedMutator bufferedMutator) throws Exception {
        String firstSeen = firstSeenHBase.getFirstSeen(message);
        String lastSeen = firstSeenHBase.getLastSeen(message);
        if (firstSeen != null && lastSeen != null) {
            LookupKey key = firstSeenHBase.getKey(message);
            Put put = new Put(key.getKey());
            put.addColumn(CF_ID, Q_KEY, key.getKey());
            put.addColumn(key.getCf(), Bytes.toBytes(FirstSeenHbaseLookup.FIRST_SEEN_PROPERTY_NAME), Bytes.toBytes(firstSeen));
            put.addColumn(key.getCf(), Bytes.toBytes(FirstSeenHbaseLookup.LAST_SEEN_PROPERTY_NAME), Bytes.toBytes(lastSeen));
            bufferedMutator.mutate(put);
        }
    }

}
