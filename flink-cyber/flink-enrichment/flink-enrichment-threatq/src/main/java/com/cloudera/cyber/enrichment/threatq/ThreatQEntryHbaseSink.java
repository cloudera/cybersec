package com.cloudera.cyber.enrichment.threatq;

import com.cloudera.cyber.hbase.AbstractHbaseSinkFunction;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.hbase.sink.HBaseMutationConverter;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

public class ThreatQEntryHbaseSink extends AbstractHbaseSinkFunction<ThreatQEntry> {
    private static final byte[] cf = Bytes.toBytes("t");

    private static final HBaseMutationConverter<ThreatQEntry>  THREATQ_HBASE_MUTATION_CONVERTER = new HBaseMutationConverter<ThreatQEntry>() {

        @Override
        public void open() {

        }

        @Override
        public Mutation convertToMutation(ThreatQEntry threatQEntry) {
            Put put = new Put(Bytes.toBytes(threatQEntry.getTq_type() + ":" + threatQEntry.getIndicator()));

            put.addColumn(cf, Bytes.toBytes("id"), Bytes.toBytes(threatQEntry.getTq_id().toString()));
            if (threatQEntry.getTq_sources() != null && threatQEntry.getTq_sources().size() > 0)
                put.addColumn(cf, Bytes.toBytes("sources"), Bytes.toBytes(String.join(",", threatQEntry.getTq_sources())));
            put.addColumn(cf, Bytes.toBytes("createdAt"), Bytes.toBytes(threatQEntry.getTq_created_at().getTime()));
            put.addColumn(cf, Bytes.toBytes("updatedAt"), Bytes.toBytes(threatQEntry.getTq_updated_at().getTime()));
            put.addColumn(cf, Bytes.toBytes("touchedAt"), Bytes.toBytes(threatQEntry.getTq_touched_at().getTime()));
            put.addColumn(cf, Bytes.toBytes("type"), Bytes.toBytes(threatQEntry.getTq_type()));
            put.addColumn(cf, Bytes.toBytes("savedSearch"), Bytes.toBytes(threatQEntry.getTq_saved_search()));
            put.addColumn(cf, Bytes.toBytes("url"), Bytes.toBytes(threatQEntry.getTq_url()));
            if (threatQEntry.getTq_tags() != null && threatQEntry.getTq_tags().size() > 0)
                put.addColumn(cf, Bytes.toBytes("tags"), Bytes.toBytes(String.join(",", threatQEntry.getTq_tags())));
            put.addColumn(cf, Bytes.toBytes("status"), Bytes.toBytes(threatQEntry.getTq_status()));
            put.addColumn(cf, Bytes.toBytes("score"), Bytes.toBytes(threatQEntry.getTq_score()));

            threatQEntry.getTq_attributes().forEach((k, v) -> put.addColumn(cf, Bytes.toBytes(k), Bytes.toBytes(v)));

            return put;
        }
    };

    public ThreatQEntryHbaseSink(String hTableName, ParameterTool params) {
        super(hTableName, THREATQ_HBASE_MUTATION_CONVERTER, params, "threatqNew");
    }
}
