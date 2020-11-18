package com.cloudera.cyber.indexing.hive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.hadoop.mapred.utils.HadoopUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.windowing.ProcessAllWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.table.catalog.exceptions.CatalogException;
import org.apache.flink.types.Row;
import org.apache.flink.util.Collector;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hive.streaming.HiveStreamingConnection;
import org.apache.hive.streaming.StreamingConnection;
import org.apache.hive.streaming.StrictJsonWriter;

import javax.annotation.Nullable;
import java.io.File;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

@Slf4j
public class HiveStreamingTransactionProcess extends ProcessAllWindowFunction<Row, ErrorRow, TimeWindow> {

    private final transient ObjectMapper objectMapper = new ObjectMapper();

    HiveStreamingConnection connection;

    private static HiveConf createHiveConf(@Nullable String hiveConfDir) {
        log.info("Setting hive conf dir as {}", hiveConfDir);

        try {
            HiveConf.setHiveSiteLocation(hiveConfDir == null ? null : Paths.get(hiveConfDir, "hive-site.xml").toUri().toURL());
        } catch (MalformedURLException var7) {
            throw new CatalogException(String.format("Failed to get hive-site.xml from %s", hiveConfDir), var7);
        }

        org.apache.hadoop.conf.Configuration hadoopConf = HadoopUtils.getHadoopConfiguration(new org.apache.flink.configuration.Configuration());
        String[] var2 = HadoopUtils.possibleHadoopConfPaths(new org.apache.flink.configuration.Configuration());
        int var3 = var2.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            String possibleHadoopConfPath = var2[var4];
            File mapredSite = new File(new File(possibleHadoopConfPath), "mapred-site.xml");
            if (mapredSite.exists()) {
                hadoopConf.addResource(new Path(mapredSite.getAbsolutePath()));
                break;
            }
        }

        return new HiveConf(hadoopConf, HiveConf.class);
    }


    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);

        StrictJsonWriter jsonWriter = StrictJsonWriter.newBuilder().build();
        if (connection == null) {
            connection = HiveStreamingConnection.newBuilder()
                    .withDatabase(parameters.getString("hive.dbname", "cyber", false))
                    .withTable(parameters.getString("hive.table", "events", false))
                    .withStreamingOptimizations(true)
                    .withTransactionBatchSize(1000)
                    .withAgentInfo("flink-cyber")
                    .withRecordWriter(jsonWriter)
                    .withHiveConf(createHiveConf("/etc/hive/conf"))
                    .connect();
        }
        log.info("Hive Connection made %s", connection.toString());
    }

    @Override
    public void close() throws Exception {
        if (connection != null) connection.close();
        super.close();
    }

    @Override
    public void process(Context context, Iterable<Row> iterable, Collector<ErrorRow> collector) throws Exception {
        log.info("Begin transaction");
        connection.beginTransaction();
        iterable.forEach(row -> {
            try {
                connection.write(serializeRow(row));
            } catch (Exception e) {
                log.info("Row failed", e);
                collector.collect(ErrorRow.builder().row(row).exception(e).build());
            }
        });
        log.info("Commit transaction");
        connection.commitTransaction();
    }

    private byte[] serializeRow(Row row) throws JsonProcessingException {
        return objectMapper.writeValueAsString(row).getBytes(StandardCharsets.UTF_8);
    }
}
