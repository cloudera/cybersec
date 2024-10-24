/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

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

    public AbstractHbaseSinkFunction(String htableName, HBaseMutationConverter<T> mutationConverter,
                                     ParameterTool params, String counterName) {
        super(htableName, HbaseConfiguration.configureHbase(), mutationConverter,
              params.getLong("hbase.sink.buffer-flush.max-size", 2),
              params.getLong("hbase.sink.buffer-flush.max-rows", 1000),
              params.getLong("hbase.sink.buffer-flush.interval", 1000));
        this.counterName = counterName;
    }

    public void open(org.apache.flink.configuration.Configuration parameters) throws Exception {
        log.info("HBase Zookeeper quorum: {}", getHBaseConfig().get("hbase.zookeeper.quorum"));
        super.open(parameters);
        this.hbaseWriteCounter = getRuntimeContext().getMetricGroup().counter(counterName);
    }

    public void invoke(T value, Context context) throws Exception {
        super.invoke(value, context);
        hbaseWriteCounter.inc();
    }
}
