package com.cloudera.cyber.hbase;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.hbase.sink.HBaseMutationConverter;
import org.apache.flink.connector.hbase.sink.HBaseSinkFunction;
import org.apache.flink.metrics.Counter;

@Slf4j
public class AbstractHbaseSinkFunction<T> extends HBaseSinkFunction<T> {

    private final String counterName;

    private transient Counter hbaseWriteCounter;

    public AbstractHbaseSinkFunction(String hTableName, HBaseMutationConverter<T> mutationConverter, ParameterTool params, String counterName) {
        super(hTableName, HbaseConfiguration.configureHbase(), mutationConverter, params.getLong("hbase.sink.buffer-flush.max-size", 2),
                params.getLong("hbase.sink.buffer-flush.max-rows", 1000),
                params.getLong("sink.buffer-flush.interval", 1000));
        this.counterName = counterName;
    }

    public void open(org.apache.flink.configuration.Configuration parameters) throws Exception {
        super.open(parameters);
        this.hbaseWriteCounter = getRuntimeContext().getMetricGroup().counter(counterName);
    }

    public void invoke(T value, Context context) throws Exception {
        super.invoke(value, context);
        hbaseWriteCounter.inc();
    }
}
