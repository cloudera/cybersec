package com.cloudera.cyber.enrichment.hbase;

import com.cloudera.cyber.EnrichmentEntry;
import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.hbase.AbstractHbaseSinkFunction;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.hbase.sink.HBaseMutationConverter;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

import static com.cloudera.cyber.enrichment.EnrichmentUtils.*;

public class HBaseEnrichmentCommandSink extends AbstractHbaseSinkFunction<EnrichmentCommand> {

    private static final HBaseMutationConverter<EnrichmentCommand> ENRICHMENT_COMMAND_TO_MUTATION = new HBaseMutationConverter<EnrichmentCommand> () {

        @Override
        public void open() {
            // nothing needed here
        }

        @Override
        public Mutation convertToMutation(EnrichmentCommand enrichmentCommand) {
            // For each incoming query result we create a Put operation
            EnrichmentEntry enrichmentEntry = enrichmentCommand.getPayload();
            switch (enrichmentCommand.getType()) {
                case ADD:
                    Put put = new Put(enrichmentKey(enrichmentEntry.getKey()));
                    put.addColumn(CF_ID, Q_KEY, Bytes.toBytes(enrichmentEntry.getKey()));
                    // add the map for the entries
                    enrichmentEntry.getEntries().forEach((k, v) -> put.addColumn(Bytes.toBytes(enrichmentEntry.getType()), Bytes.toBytes(k), Bytes.toBytes(v)));
                    return put;
                case DELETE:
                    return new Delete(Bytes.toBytes(enrichmentEntry.getKey()));
            }

            // this should not happen - Enrichment commands filtered into ADD and DELETE before sink
            return null;
        }
    };

    public HBaseEnrichmentCommandSink(String hTableName, ParameterTool params) {
        super(hTableName, ENRICHMENT_COMMAND_TO_MUTATION,params, "numEnrichmentsWritten");
    }
}
