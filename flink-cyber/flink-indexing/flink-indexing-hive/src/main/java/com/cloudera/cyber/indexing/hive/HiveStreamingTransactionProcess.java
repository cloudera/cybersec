package com.cloudera.cyber.indexing.hive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.windowing.ProcessAllWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.types.Row;
import org.apache.flink.util.Collector;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hive.streaming.HiveStreamingConnection;
import org.apache.hive.streaming.StreamingConnection;
import org.apache.hive.streaming.StreamingException;
import org.apache.hive.streaming.StrictJsonWriter;

import java.nio.charset.StandardCharsets;

public class HiveStreamingTransactionProcess extends ProcessAllWindowFunction<Row, ErrorRow, TimeWindow> {

    private final transient ObjectMapper objectMapper = new ObjectMapper();

    StreamingConnection connection;

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        StrictJsonWriter jsonWriter = StrictJsonWriter.newBuilder().build();
        HiveConf hiveConf = new HiveConf();
        connection = HiveStreamingConnection.newBuilder()
                .withDatabase(parameters.getString("hive.dbname", "cyber", true))
                .withTable(parameters.getString("hive.table", "events", true))
                .withStreamingOptimizations(true)
                .withRecordWriter(jsonWriter)
                .withHiveConf(hiveConf)
                .connect();
    }

    @Override
    public void close() throws Exception {
        super.close();
        connection.close();
    }

    @Override
    public void process(Context context, Iterable<Row> iterable, Collector<ErrorRow> collector) throws Exception {
        connection.beginTransaction();
        iterable.forEach(row -> {
            try {
                connection.write(serializeRow(row));
            } catch (StreamingException | JsonProcessingException e) {
                collector.collect(ErrorRow.builder().row(row).exception(e).build());
            }
        });
        connection.commitTransaction();
    }

    private byte[] serializeRow(Row row) throws JsonProcessingException {
        return objectMapper.writeValueAsString(row).getBytes(StandardCharsets.UTF_8);
    }
}
