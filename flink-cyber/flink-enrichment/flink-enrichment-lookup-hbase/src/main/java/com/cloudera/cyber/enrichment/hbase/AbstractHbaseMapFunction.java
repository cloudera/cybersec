package com.cloudera.cyber.enrichment.hbase;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.CacheMetrics;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Counter;
import org.apache.flink.metrics.MetricGroup;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toMap;

@Slf4j
public abstract class AbstractHbaseMapFunction extends RichMapFunction<Message, Message> {
    private transient org.apache.hadoop.conf.Configuration hbaseConfig;
    protected transient MetricGroup metricsGroup;
    protected transient Counter messageCounter;
    protected transient Counter fetchCounter;
    protected transient Counter emptyResultCounter;
    protected transient Counter realResultCounter;
    private transient Connection connection;

    protected abstract String getTableName();

    private Cache<LookupKey, Map<String, String>> cache;

    private final Map<String, String> fetch(LookupKey key) {
        try {
            fetchCounter.inc();

            Get get = new Get(key.getKey());
            get.addFamily(key.getCf());

            Table table = connection.getTable(TableName.valueOf(getTableName()));
            Result result = table.get(get);
            if (result.isEmpty()) {
                emptyResultCounter.inc();
                return Collections.emptyMap();
            }
            realResultCounter.inc();
            return result.getFamilyMap(key.getCf()).entrySet().stream()
                    .collect(toMap(
                            k -> new String(k.getKey()),
                            v -> new String(v.getValue())
                    ));
        } catch (IOException e) {
            log.error("Error with HBase fetch", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public final void open(Configuration parameters) throws Exception {
        super.open(parameters);
        metricsGroup = getRuntimeContext().getMetricGroup().addGroup("hbaseCache");
        this.messageCounter = metricsGroup.counter("message");
        this.emptyResultCounter = metricsGroup.counter("emptyResult");
        this.realResultCounter = metricsGroup.counter("realResult");
        this.fetchCounter = metricsGroup.counter("fetchCounter");

        org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.conf.Configuration();
        conf.addResource("/etc/hbase/conf/hbase-site.xml");
        hbaseConfig = HBaseConfiguration.create(conf);
        connection = ConnectionFactory.createConnection(hbaseConfig);

        cache = Caffeine.newBuilder()
                .expireAfterAccess(60, TimeUnit.SECONDS)
                .maximumSize(1000)
                .recordStats(() -> new CacheMetrics(metricsGroup))
                .build();

    }

    protected final Map<String, String> notFound() {
        return Collections.<String, String>emptyMap();
    }

    public final Map<String, String> hbaseLookup(long ts, LookupKey key, String prefix) {
        return cache.get(key, this::fetch).entrySet().stream()
                .collect(toMap(
                        k -> prefix + "." + k.getKey(),
                        v -> v.getValue())
                );
    }
}

