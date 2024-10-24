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

package com.cloudera.cyber.flink;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.github.benmanes.caffeine.cache.stats.StatsCounter;
import org.apache.flink.metrics.Counter;
import org.apache.flink.metrics.MetricGroup;

public class CacheMetrics implements StatsCounter {
    private final transient MetricGroup hbaseCache;
    private final transient Counter hits;
    private final transient Counter miss;
    private final transient Counter evict;

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
