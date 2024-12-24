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

package com.cloudera.cyber.enrichment.rest;

import com.github.benmanes.caffeine.cache.Expiry;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

public class RestRequestCacheExpiry implements Expiry<RestRequestKey, RestRequestResult> {

    private final long successCacheDurationNanos;
    private final long failureCacheDurationNanos;

    public RestRequestCacheExpiry(long successCacheDuration, TimeUnit successCacheDurationUnits,
                                  long failureCacheDuration, TimeUnit failureCacheDurationUnits) {
        this.successCacheDurationNanos = TimeUnit.NANOSECONDS.convert(successCacheDuration, successCacheDurationUnits);
        this.failureCacheDurationNanos = TimeUnit.NANOSECONDS.convert(failureCacheDuration, failureCacheDurationUnits);
    }

    @Override
    public long expireAfterCreate(@Nonnull RestRequestKey cacheKey, @Nonnull RestRequestResult cacheValue,
                                  long cacheDuration) {
        if (cacheValue.getErrors().isEmpty()) {
            return successCacheDurationNanos;
        } else {
            return failureCacheDurationNanos;
        }
    }

    @Override
    public long expireAfterUpdate(@Nonnull RestRequestKey cacheKey, @Nonnull RestRequestResult cacheValue,
                                  long currentTime, long cacheDuration) {
        return cacheDuration;
    }

    @Override
    public long expireAfterRead(@Nonnull RestRequestKey cacheKey, @Nonnull RestRequestResult cacheValue,
                                long currentTime, long cacheDuration) {
        return cacheDuration;
    }

}
