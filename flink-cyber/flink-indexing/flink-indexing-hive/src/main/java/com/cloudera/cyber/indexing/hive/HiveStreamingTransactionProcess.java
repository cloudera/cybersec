package com.cloudera.cyber.indexing.hive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.windowing.ProcessAllWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.types.Row;
import org.apache.flink.util.Collector;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hive.streaming.HiveStreamingConnection;
import org.apache.hive.streaming.StreamingConnection;
import org.apache.hive.streaming.StrictJsonWriter;

import java.nio.charset.StandardCharsets;

@Slf4j
public class HiveStreamingTransactionProcess extends ProcessAllWindowFunction<Row, ErrorRow, TimeWindow> {

    private final transient ObjectMapper objectMapper = new ObjectMapper();

    StreamingConnection connection;

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);

        StrictJsonWriter jsonWriter = StrictJsonWriter.newBuilder().build();
        HiveConf hiveConf = new HiveConf();
        if (connection == null) {
            connection = HiveStreamingConnection.newBuilder()
                    .withDatabase(parameters.getString("hive.dbname", "cyber", false))
                    .withTable(parameters.getString("hive.table", "events", false))
                    .withStreamingOptimizations(true)
                    .withTransactionBatchSize(1000)
                    .withAgentInfo("flink-cyber")
                    .withRecordWriter(jsonWriter)
                    .withHiveConf(hiveConf)
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
        connection.beginTransaction();
        iterable.forEach(row -> {
            try {
                connection.write(serializeRow(row));
            } catch (Exception e) {
                collector.collect(ErrorRow.builder().row(row).exception(e).build());
            }
        });
        connection.commitTransaction();
    }

    private byte[] serializeRow(Row row) throws JsonProcessingException {
        return objectMapper.writeValueAsString(row).getBytes(StandardCharsets.UTF_8);
    }
}
