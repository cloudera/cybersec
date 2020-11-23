package com.cloudera.cyber.indexing.hive;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.formats.json.JsonRowSerializationSchema;
import org.apache.flink.metrics.Counter;
import org.apache.flink.streaming.api.functions.windowing.ProcessAllWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.table.catalog.exceptions.CatalogException;
import org.apache.flink.types.Row;
import org.apache.flink.util.Collector;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hive.streaming.HiveStreamingConnection;
import org.apache.hive.streaming.StreamingException;
import org.apache.hive.streaming.StrictJsonWriter;

import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.nio.file.Paths;

@Slf4j
public class HiveStreamingTransactionProcess extends ProcessAllWindowFunction<Row, ErrorRow, TimeWindow> {

    private final transient ObjectMapper objectMapper = new ObjectMapper();
    private final TypeInformation<Row> typeInfo;

    private transient HiveStreamingConnection connection;
    private transient JsonRowSerializationSchema jsonRowSerializationSchema;
    private transient Counter rows;

    public HiveStreamingTransactionProcess(TypeInformation<Row> typeInfo) {
        super();
        this.typeInfo = typeInfo;
    }

    private static HiveConf createHiveConf(@Nullable String hiveConfDir) {
        log.info("Setting hive conf dir as {}", hiveConfDir);
        try {
            HiveConf.setHiveSiteLocation(hiveConfDir == null ? null : Paths.get(hiveConfDir, "hive-site.xml").toUri().toURL());
        } catch (MalformedURLException var7) {
            throw new CatalogException(String.format("Failed to get hive-site.xml from %s", hiveConfDir), var7);
        }
        org.apache.hadoop.conf.Configuration hadoopConf = new org.apache.hadoop.conf.Configuration();
        return new HiveConf(hadoopConf, HiveConf.class);
    }


    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);

        rows = getRuntimeContext().getMetricGroup().counter("rowsToHive");

        StrictJsonWriter jsonWriter = StrictJsonWriter.newBuilder().build();

        connection = HiveStreamingConnection.newBuilder()
                .withDatabase(parameters.getString("hive.dbname", "cyber", false))
                .withTable(parameters.getString("hive.table", "events", false))
                //.withStreamingOptimizations(true)
                .withTransactionBatchSize(parameters.getInteger("hive.batch.size", 1000))
                .withAgentInfo("flink-cyber")
                .withRecordWriter(jsonWriter)
                .withHiveConf(createHiveConf("/etc/hive/conf"))
                .connect();

        log.info("Hive Connection made {}", connection);
    }

    @Override
    public void close() throws Exception {
        if (connection != null) connection.close();
        super.close();
    }

    @Override
    public void process(Context context, Iterable<Row> iterable, Collector<ErrorRow> collector) throws Exception {
        log.info("Begin transaction");
        try {
            connection.beginTransaction();

            iterable.forEach(row -> {
                rows.inc();
                try {
                    byte[] json = serializeRow(row);
                    connection.write(json);
                    log.info(String.format("Writing row to hive %s", new String(json)));
                } catch (StreamingException e) {
                    log.error(String.format("StreamingException writing row: %s", row), e);
                    collector.collect(ErrorRow.builder().row(row).exception(e).build());
                } catch (Exception e) {
                    log.error(String.format("Error writing row: %s", row), e);
                }
            });
            log.info("Commit transaction");
            connection.commitTransaction();
        } catch (StreamingException e) {
            log.error("Transactional Streaming Exception", e);
            throw (e);
        }
    }

    @VisibleForTesting
    byte[] serializeRow(Row row) {
        if (jsonRowSerializationSchema == null)
            jsonRowSerializationSchema = new JsonRowSerializationSchema(typeInfo);
        return jsonRowSerializationSchema.serialize(row);
    }
}
