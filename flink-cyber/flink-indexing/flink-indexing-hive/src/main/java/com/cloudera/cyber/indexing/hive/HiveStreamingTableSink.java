package com.cloudera.cyber.indexing.hive;

import com.cloudera.cyber.flink.EventTimeAndCountTrigger;
import com.sun.istack.NotNull;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.ValidationException;
import org.apache.flink.table.sinks.AppendStreamTableSink;
import org.apache.flink.table.sinks.TableSink;
import org.apache.flink.types.Row;
import org.apache.parquet.Strings;

import java.util.Arrays;

@EqualsAndHashCode
@Slf4j
public class HiveStreamingTableSink implements AppendStreamTableSink<Row> {

    @NotNull private TableSchema schema;
    @NonNull private String schemaName;
    @NonNull private String table;

    public HiveStreamingTableSink(TableSchema schema, @NonNull String schemaName, @NonNull String table) {
        log.info(String.format("Constructing: %s, %s, %s", schema.toString(), schemaName, table));
        this.schema = schema;
        this.schemaName = schemaName;
        this.table = table;
    }

    @Override
    public void emitDataStream(DataStream<Row> dataStream) {
    }

    @Override
    public DataStreamSink<Row> consumeDataStream(DataStream<Row> dataStream) {
        long batchTime = 1000;
        long maxEvents = 10000;
        dataStream.windowAll(TumblingProcessingTimeWindows.of(Time.milliseconds(batchTime)))
                .trigger(EventTimeAndCountTrigger.of(maxEvents))
                .process(new HiveStreamingTransactionProcess());
        return null;
    }

    @Override
    public TableSink<Row> configure(String[] fieldNames, TypeInformation<?>[] fieldTypes) {
        log.info("Configuring Table sink " + Strings.join(fieldNames, ",") + fieldNames.toString());
        if (Arrays.equals(this.getFieldNames(), fieldNames) && Arrays.equals(this.getFieldTypes(), fieldTypes)) {
            return this;
        } else {
            throw new ValidationException("Reconfiguration with different fields is not allowed. Expected: " + Arrays.toString(this.getFieldNames()) + " / " + Arrays.toString(this.getFieldTypes()) + ". " + "But was: " + Arrays.toString(fieldNames) + " / " + Arrays.toString(fieldTypes));
        }
    }

    public TableSchema getTableSchema() {
        return schema;
    }

}
