package com.cloudera.cyber.indexing.hive;

import com.cloudera.cyber.flink.EventTimeAndCountTrigger;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.ValidationException;
import org.apache.flink.table.sinks.AppendStreamTableSink;
import org.apache.flink.table.sinks.TableSink;
import org.apache.flink.table.types.DataType;
import org.apache.flink.types.Row;
import org.apache.parquet.Strings;

import java.util.Arrays;

@EqualsAndHashCode
@Slf4j
public class HiveStreamingTableSink implements AppendStreamTableSink<Row> {

    @NonNull private TableSchema schema;
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
        consumeDataStream(dataStream);
    }

    @Override
    public DataStreamSink<Row> consumeDataStream(DataStream<Row> dataStream) {
        long batchTime = 1000;
        long maxEvents = 10000;

        return dataStream.windowAll(TumblingProcessingTimeWindows.of(Time.milliseconds(batchTime)))
                .trigger(EventTimeAndCountTrigger.of(maxEvents))
                .process(new HiveStreamingTransactionProcess(getOutputType())).name("Hive Stream Process").uid("hive-process")
                .map(e -> e.row).name("Error Mapper").uid("error-map")
                .addSink(new SinkFunction<Row>() {
                    @Override
                    public void invoke(Row value, Context context) throws Exception {
                    log.error("Error Row: " + value.toString());
                    }
                }).name("Error handler").uid("error-sink");
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

    @Override
    public DataType getConsumedDataType() {
        return getTableSchema().toRowDataType();
    }

    @Override
    public TypeInformation<Row> getOutputType() {
        return new RowTypeInfo(schema.getFieldTypes(), schema.getFieldNames());
    }
}
