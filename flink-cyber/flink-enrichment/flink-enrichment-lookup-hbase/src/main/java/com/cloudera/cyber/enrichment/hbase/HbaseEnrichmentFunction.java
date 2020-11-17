package com.cloudera.cyber.enrichment.hbase;

import org.apache.flink.table.functions.ScalarFunction;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

/**
 * Return a lookup for a given Hbase enrichment at a given time
 * <p>
 * This function also performs local positive result caching
 *
 * @TODO - At present the timestamp is ignored, and only latest values are returned, this is for consistency with Metron behavior
 * @TODO - Figure out how to invalidate local negative caching with a signal from the loader to indicate HBase enrichment changes
 * @TODO - Consider how all this would be converted to an Async implementation
 */
public class HbaseEnrichmentFunction extends ScalarFunction {

    private static final TableName hbaseTable = TableName.valueOf("enrichments");

    public Map<String, String> eval(long timestamp, String type, String key) {
        try (Connection connection = ConnectionFactory.createConnection()) {
            byte[] cf = Bytes.toBytes(type);
            Get get = new Get(Bytes.toBytes(key)).addFamily(cf);

            Table table = connection.getTable(hbaseTable);
            Result result = table.get(get);

            if (!result.getExists())
                return Collections.emptyMap();

            return result.getFamilyMap(cf).entrySet().stream()
                    .collect(toMap(
                            k -> new String(k.getKey()),
                            v -> new String(v.getValue())
                    ));
        } catch (
                IOException e) {
            return Collections.emptyMap();
        }
    }
}