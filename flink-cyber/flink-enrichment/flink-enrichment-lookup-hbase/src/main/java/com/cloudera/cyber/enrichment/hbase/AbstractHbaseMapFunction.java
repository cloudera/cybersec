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
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Slf4j
public abstract class AbstractHbaseMapFunction extends RichMapFunction<Message, Message> {
    private static final Map<String, Function<byte[],String>> columnConversionFunction = new HashMap<String, Function<byte[],String>>() {{
        put("updatedAt", AbstractHbaseMapFunction::longBytesToString);
        put("createdAt", AbstractHbaseMapFunction::longBytesToString);
        put("touchedAt", AbstractHbaseMapFunction::longBytesToString);
        put("score", AbstractHbaseMapFunction::floatBytesToString);
    }};
    private transient org.apache.hadoop.conf.Configuration hbaseConfig;
    protected transient MetricGroup metricsGroup;
    protected transient Counter messageCounter;
    protected transient Counter fetchCounter;
    protected transient Counter emptyResultCounter;
    protected transient Counter realResultCounter;
    private transient Connection connection;

    protected abstract String getTableName();

    private Cache<LookupKey, Map<String, String>> cache;

    protected Map<String, String> fetch(LookupKey key) {
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
            return result.getFamilyMap(key.getCf()).entrySet().stream().map(AbstractHbaseMapFunction::hbaseBytesToString)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } catch (IOException e) {
            log.error("Error with HBase fetch", e);
            throw new RuntimeException(e);
        }
    }

    private static Map.Entry<String, String> hbaseBytesToString(Map.Entry<byte[], byte[]> e) {
        String keyString = Bytes.toString(e.getKey());
        Function<byte[], String> conversion = columnConversionFunction.getOrDefault(keyString, AbstractHbaseMapFunction::stringBytesToString);
        return new AbstractMap.SimpleEntry<>(keyString, conversion.apply(e.getValue()));
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

    @Override
    public final void open(Configuration parameters) throws Exception {
        super.open(parameters);
        metricsGroup = getRuntimeContext().getMetricGroup().addGroup("hbaseCache");
        this.messageCounter = metricsGroup.counter("message");
        this.emptyResultCounter = metricsGroup.counter("emptyResult");
        this.realResultCounter = metricsGroup.counter("realResult");
        this.fetchCounter = metricsGroup.counter("fetchCounter");

        hbaseConfig =  configureHbase();
        connection = ConnectionFactory.createConnection(hbaseConfig);

        cache = Caffeine.newBuilder()
                .expireAfterAccess(60, TimeUnit.SECONDS)
                .maximumSize(1000)
                .recordStats(() -> new CacheMetrics(metricsGroup))
                .build();

    }

    public static org.apache.hadoop.conf.Configuration configureHbase() {
        org.apache.hadoop.conf.Configuration hbaseClientConf = HBaseConfiguration.create();
        String hbaseConfDir = "/etc/hbase/conf";
        if ((new File(hbaseConfDir)).exists()) {
            String coreSite = hbaseConfDir + "/core-site.xml";
            String hdfsSite = hbaseConfDir + "/hdfs-site.xml";
            String hbaseSite = hbaseConfDir + "/hbase-site.xml";
            if ((new File(coreSite)).exists()) {
                hbaseClientConf.addResource(new Path(coreSite));
                log.info("Adding " + coreSite + " to hbase configuration");
            }

            if ((new File(hdfsSite)).exists()) {
                hbaseClientConf.addResource(new Path(hdfsSite));
                log.info("Adding " + hdfsSite + " to hbase configuration");
            }

            if ((new File(hbaseSite)).exists()) {
                hbaseClientConf.addResource(new Path(hbaseSite));
                log.info("Adding " + hbaseSite + " to hbase configuration");
            }
        } else {
            log.warn("HBase config directory '{}' not found, cannot load HBase configuration.", hbaseConfDir);
        }

        return hbaseClientConf;
    }

    protected final Map<String, String> notFound() {
        return Collections.emptyMap();
    }

    public final Map<String, String> hbaseLookup(long ts, LookupKey key, String prefix) {
        return cache.get(key, this::fetch).entrySet().stream()
                .collect(toMap(
                        k -> prefix + "." + k.getKey(),
                        Map.Entry::getValue)
                );
    }
}

