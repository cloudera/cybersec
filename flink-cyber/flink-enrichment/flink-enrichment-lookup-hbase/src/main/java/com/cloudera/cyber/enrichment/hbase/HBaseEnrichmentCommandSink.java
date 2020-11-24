package com.cloudera.cyber.enrichment.hbase;

import com.cloudera.cyber.EnrichmentEntry;
import com.cloudera.cyber.commands.EnrichmentCommand;
import org.apache.flink.addons.hbase.HBaseSinkFunction;
import org.apache.hadoop.hbase.client.BufferedMutator;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

import static com.cloudera.cyber.enrichment.EnrichmentUtils.*;

public class HBaseEnrichmentCommandSink extends HBaseSinkFunction<EnrichmentCommand> {

    public HBaseEnrichmentCommandSink(String hTableName) {
        super(hTableName);
    }

    public void executeMutations(EnrichmentCommand enrichmentCommand, Context context, BufferedMutator mutator) throws Exception {
        // For each incoming query result we create a Put operation
        EnrichmentEntry enrichmentEntry = enrichmentCommand.getPayload();
        switch (enrichmentCommand.getType()) {
            case ADD:
                Put put = new Put(enrichmentKey(enrichmentEntry.getKey()));
                put.addColumn(CF_ID, Q_KEY, Bytes.toBytes(enrichmentEntry.getKey()));
                // add the map for the entries
                enrichmentEntry.getEntries().forEach((k, v) -> put.addColumn(Bytes.toBytes(enrichmentEntry.getType()), Bytes.toBytes(k), Bytes.toBytes(v)));
                mutator.mutate(put);
                break;
            case DELETE:
                Delete delete = new Delete(Bytes.toBytes(enrichmentEntry.getKey()));
                mutator.mutate(delete);
                break;
        }
    }
}
