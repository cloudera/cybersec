package com.cloudera.cyber.enrichment.hbase.mutators;

import com.cloudera.cyber.EnrichmentEntry;
import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageConfig;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

import static com.cloudera.cyber.enrichment.EnrichmentUtils.*;

public class SimpleHbaseEnrichmentMutationConverter implements EnrichmentCommandMutationConverter {
    @Override
    public Mutation convertToMutation(EnrichmentStorageConfig storageConfig, EnrichmentCommand enrichmentCommand) {
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
}
