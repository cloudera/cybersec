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

import com.cloudera.cyber.flink.CacheMetrics;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.hbase.util.HBaseConfigurationUtil;
import org.apache.flink.metrics.Counter;
import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.util.StringUtils;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

@Slf4j
public abstract class AbstractHbaseMapFunction<IN, OUT> extends RichMapFunction<IN, OUT> {
    private static final Map<String, Function<byte[],String>> columnConversionFunction = new HashMap<String, Function<byte[],String>>() {{
        put("updatedAt", AbstractHbaseMapFunction::longBytesToString);
        put("createdAt", AbstractHbaseMapFunction::longBytesToString);
        put("touchedAt", AbstractHbaseMapFunction::longBytesToString);
        put("score", AbstractHbaseMapFunction::floatBytesToString);
    }};
    private final byte[] serializedHbaseConfig;
    protected transient MetricGroup metricsGroup;
    protected transient Counter messageCounter;
    protected transient Counter fetchCounter;
    protected transient Counter emptyResultCounter;
    protected transient Counter realResultCounter;
    private transient Connection connection;

    private Cache<LookupKey, Map<String, Object>> cache;

    protected Map<String, Object> fetch(LookupKey key) {
        try {
            fetchCounter.inc();

            Get get = key.toGet();

            Table table = connection.getTable(TableName.valueOf(key.getTableName()));
            Result result = table.get(get);
            if (result.isEmpty()) {
                emptyResultCounter.inc();
                return Collections.emptyMap();
            }
            realResultCounter.inc();

            return key.resultToMap(result);
        } catch (IOException e) {
            log.error("Error with HBase fetch", e);
            throw new RuntimeException(e);
        }
    }

    private static String longBytesToString(byte[] bytesValue) {
        return Long.toString(Bytes.toLong(bytesValue));
    }

    private static String floatBytesToString(byte[] bytesValue) {
        return Float.toString(Bytes.toFloat(bytesValue));
    }

    private static String stringBytesToString(byte[] bytesValue) {
        return Bytes.toString(bytesValue);
    }

    public AbstractHbaseMapFunction() {
        serializedHbaseConfig = HBaseConfigurationUtil.serializeConfiguration(HbaseConfiguration.configureHbase());
    }

    @Override
    public final void open(Configuration parameters) throws Exception {
        super.open(parameters);
        metricsGroup = getRuntimeContext().getMetricGroup().addGroup("hbaseCache");
        this.messageCounter = metricsGroup.counter("message");
        this.emptyResultCounter = metricsGroup.counter("emptyResult");
        this.realResultCounter = metricsGroup.counter("realResult");
        this.fetchCounter = metricsGroup.counter("fetchCounter");

        org.apache.hadoop.conf.Configuration hbaseConfig = HBaseConfigurationUtil.deserializeConfiguration(serializedHbaseConfig, HbaseConfiguration.configureHbase());
        log.info("Start connection to hbase zookeeper quorum: {}",hbaseConfig.get("hbase.zookeeper.quorum"));
        connection = ConnectionFactory.createConnection(hbaseConfig);
        log.info("Connected to hbase zookeeper quorum: {}",hbaseConfig.get("hbase.zookeeper.quorum"));

        cache = Caffeine.newBuilder()
                .expireAfterAccess(60, TimeUnit.SECONDS)
                .maximumSize(1000)
                .recordStats(() -> new CacheMetrics(metricsGroup))
                .build();

    }

    protected final Map<String, String> notFound() {
        return Collections.emptyMap();
    }

    private static String objectToString(Object objectValue) {
        return objectValue != null ? objectValue.toString() : "";
    }

    public final Map<String, String> hbaseLookup(long ts, LookupKey key, String prefix) {
        return Objects.requireNonNull(cache.get(key, this::fetch)).entrySet().stream()
                .collect(toMap(
                        k -> prefix + "." + k.getKey(),
                        v -> objectToString(v.getValue()))
                );
    }

}

