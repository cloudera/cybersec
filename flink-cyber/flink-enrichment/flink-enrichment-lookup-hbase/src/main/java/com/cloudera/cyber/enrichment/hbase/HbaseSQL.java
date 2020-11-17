package com.cloudera.cyber.enrichment.hbase;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentConfig;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentKind;
import org.apache.avro.Schema;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Constructs SQL to enrichment a message passing through all messages
 *
 * Note that the enrichments will be pulled from a temporal table function and that the function needs to return
 * an empty map if there is no enrichment found in order to avoid left joins, since the end result of the enrichment
 * absolutely has to be an appendableStream.
 */
public class HbaseSQL {
    private static Stream<EnrichmentConfig> configStream(List<EnrichmentConfig> configs) {
        return configs.stream()
                .filter(e -> {
                    return e.getKind().equals(EnrichmentKind.HBASE);
                });
    }

    static String buildSql(List<EnrichmentConfig> configs) {
        String outputFields = streamMessageFields()
                .map(f -> f.name())
                .collect(Collectors.joining("," ));

        String extensionsSql = configStream(configs)
                .flatMap(e ->
                        e.getFields().stream().map(f -> {
                                    String prefix = f.getName() + "." + f.getEnrichmentType();
                            String enrichmentFunction = String.format("HBASE_ENRICHMENT(messages.ts, '%s', messages.extensions['%s'])", f.getEnrichmentType(), f.getName());
                            String sql = String.format("MAP_PREFIX(%s, '%s')", enrichmentFunction, prefix);
                                    return sql;
                                }
                        )
                )
                .collect(Collectors.joining(", "));

        String inputFields = streamMessageFields()
                .map(f -> f.name().equals("extensions") ?
                        String.format("MAP_MERGE(messages.extensions, %s) as extensions", extensionsSql) :
                        String.format("messages.%s", f.name()))
                .collect(Collectors.joining(","));

        return String.format("select %s from messages", outputFields, inputFields);
    }

    private static Stream<Schema.Field> streamMessageFields() {
        return Message.getClassSchema().getFields().stream();
    }
}
