package com.cloudera.cyber.flink;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.github.benmanes.caffeine.cache.stats.StatsCounter;
import org.apache.flink.metrics.Counter;
import org.apache.flink.metrics.MetricGroup;

public class CacheMetrics implements StatsCounter {
    private transient final MetricGroup hbaseCache;
    private transient final Counter hits;
    private transient final Counter miss;
    private transient final Counter evict;

    public CacheMetrics(MetricGroup hbaseCache) {
        this.hbaseCache = hbaseCache;
        this.hits = hbaseCache.counter("hits");
        this.miss = hbaseCache.counter("misses");
        this.evict = hbaseCache.counter("evictions");
    }

    @Override
    public void recordHits(int count) {
        hits.inc(count);
    }

    @Override
    public void recordMisses(int count) {
        miss.inc(count);
    }

    @Override
    public void recordLoadSuccess(long loadTime) {
    }

    @Override
    public void recordLoadFailure(long loadTime) {
    }

    @Override
    public void recordEviction() {
        evict.inc();
    }

    @Override
    public void recordEviction(int weight) {
        evict.inc();
    }

    @Override
    public CacheStats snapshot() {
        return new CacheStats(hits.getCount(), miss.getCount(), 0, 0, 0, evict.getCount(), 0);
    }
}
