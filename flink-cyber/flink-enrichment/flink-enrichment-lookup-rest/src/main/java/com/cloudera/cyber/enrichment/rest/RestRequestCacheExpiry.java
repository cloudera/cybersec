package com.cloudera.cyber.enrichment.rest;

import com.github.benmanes.caffeine.cache.Expiry;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

public class RestRequestCacheExpiry implements Expiry<RestRequestKey, RestRequestResult> {

    private final long successCacheDurationNanos;
    private final long failureCacheDurationNanos;

    public RestRequestCacheExpiry(long successCacheDuration, TimeUnit successCacheDurationUnits, long failureCacheDuration, TimeUnit failureCacheDurationUnits) {
        this.successCacheDurationNanos = TimeUnit.NANOSECONDS.convert(successCacheDuration, successCacheDurationUnits);
        this.failureCacheDurationNanos = TimeUnit.NANOSECONDS.convert(failureCacheDuration, failureCacheDurationUnits);
    }

    @Override
    public long expireAfterCreate(@Nonnull RestRequestKey cacheKey, @Nonnull RestRequestResult cacheValue, long cacheDuration) {
        if (cacheValue.getErrors().isEmpty()) {
            return successCacheDurationNanos;
        } else {
            return failureCacheDurationNanos;
        }
    }

    @Override
    public long expireAfterUpdate(@Nonnull RestRequestKey cacheKey, @Nonnull RestRequestResult cacheValue, long currentTime, long cacheDuration) {
        return cacheDuration;
    }

    @Override
    public long expireAfterRead(@Nonnull RestRequestKey cacheKey, @Nonnull RestRequestResult cacheValue, long currentTime, long cacheDuration) {
        return cacheDuration;
    }

}
