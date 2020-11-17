package com.cloudera.cyber.enrichment.hbase;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentConfig;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentField;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentKind;
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
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Slf4j
public class HbaseEnrichmentMapFunction extends RichMapFunction<Message, Message> {
    private final Map<String, List<EnrichmentField>> fieldToLookup;
    private final Set<String> sources;
    private transient Table table;
    private transient MetricGroup metricsGroup;
    private transient Counter messageCounter;

    public Map<String, String> fetch(LookupKey key) {
        try {
            Get get = new Get(key.getKey());
            get.addFamily(key.getCf());

            Result result = table.get(get);
            if (!result.getExists())
                return Collections.emptyMap();
            return result.getFamilyMap(key.getCf()).entrySet().stream()
                    .collect(toMap(
                            k -> new String(k.getKey()),
                            v -> new String(v.getValue())
                    ));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    Cache<LookupKey, Map<String, String>> cache;

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        metricsGroup = getRuntimeContext().getMetricGroup().addGroup("hbaseCache");
        this.messageCounter = metricsGroup.counter("message");
        cache = Caffeine.newBuilder()
                .expireAfterAccess(60, TimeUnit.SECONDS)
                .maximumSize(1000)
                .recordStats(() -> new CacheMetrics(metricsGroup))
                .build();
    }

    public HbaseEnrichmentMapFunction(List<EnrichmentConfig> configs, String tableName) {
        sources = configs.stream()
                .filter(c -> c.getKind().equals(EnrichmentKind.HBASE))
                .map(c -> c.getSource())
                .collect(Collectors.toSet());

        fieldToLookup = configs.stream()
                .filter(c -> c.getKind().equals(EnrichmentKind.HBASE))
                .collect(toMap(
                        k -> k.getSource(), v -> v.getFields())
                );

        log.info("Applying HBase enrichments to the following sources: %s", sources);
        org.apache.hadoop.conf.Configuration conf = HBaseConfiguration.create();
        try (Connection connection = ConnectionFactory.createConnection(conf)) {
            table = connection.getTable(TableName.valueOf(tableName));
        } catch (IOException e) {
            log.error("Error with HBase", e);
        }
    }

    @Override
    public Message map(Message message) {
        if (sources.stream().noneMatch(s -> message.getSource().equals(s))) return message;

        messageCounter.inc(1);
        
        return MessageUtils.addFields(message, fieldToLookup.get(message.getSource()).stream().map(
                field -> {
                    byte[] cf = Bytes.toBytes(field.getEnrichmentType());
                    String key = message.getExtensions().get(field.getName());
                    return ((key == null) ? notFound() : hbaseLookup(message.getTs(),
                            LookupKey.builder()
                                    .key(Bytes.toBytes(key))
                                    .cf(cf)
                                    .build(),
                            field.getName())).entrySet().stream();
                }).flatMap(l -> l)
                .collect(toMap(k -> k.getKey(), v -> v.getValue(), (a, b) -> b)));
    }

    private Map<String, String> notFound() {
        return Collections.<String, String>emptyMap();
    }

    private Map<String, String> hbaseLookup(long ts, LookupKey key, String prefix) {
        return cache.get(key, this::fetch).entrySet().stream()
                .collect(toMap(
                        k -> prefix + "." + k.getKey(),
                        v -> v.getValue())
                );
    }
}
