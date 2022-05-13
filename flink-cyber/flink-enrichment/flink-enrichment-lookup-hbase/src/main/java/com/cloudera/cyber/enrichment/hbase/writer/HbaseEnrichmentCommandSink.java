package com.cloudera.cyber.enrichment.hbase.writer;

import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageConfig;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentsConfig;
import com.cloudera.cyber.enrichment.hbase.mutators.HbaseEnrichmentMutationConverter;
import com.cloudera.cyber.hbase.AbstractHbaseSinkFunction;
import org.apache.flink.api.java.utils.ParameterTool;

public class HbaseEnrichmentCommandSink extends AbstractHbaseSinkFunction<EnrichmentCommand>  {
    public HbaseEnrichmentCommandSink(String hTableName, EnrichmentsConfig enrichmentsConfig, ParameterTool params) {
        super(hTableName, new HbaseEnrichmentMutationConverter(enrichmentsConfig), params, "numEnrichmentsWritten");
    }
}
