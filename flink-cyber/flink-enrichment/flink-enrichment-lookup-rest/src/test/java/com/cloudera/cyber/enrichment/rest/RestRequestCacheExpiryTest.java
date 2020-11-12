package com.cloudera.cyber.enrichment.rest;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class RestRequestCacheExpiryTest {

    @Test
    public void testExpiry() {
        long successHours = 1;
        TimeUnit successUnit = TimeUnit.HOURS;

        long failureMinutes = 10;
        TimeUnit failureUnit = TimeUnit.MINUTES;

        RestRequestCacheExpiry expiry = new RestRequestCacheExpiry(successHours, successUnit, failureMinutes,failureUnit);
        RestRequestKey key = new RestRequestKey(Collections.emptyMap(), "http://host:1234/path");

        RestRequestResult successResult = new RestRequestResult();
        Assert.assertEquals(TimeUnit.NANOSECONDS.convert(successHours, successUnit), expiry.expireAfterCreate(key, successResult, 0));

        RestRequestResult failureResult = new RestRequestResult(new HashMap<>(), Collections.singletonList("error message"));
        Assert.assertEquals(TimeUnit.NANOSECONDS.convert(failureMinutes, failureUnit), expiry.expireAfterCreate(key, failureResult, 0));

        long expectedDuration = 3;
        Assert.assertEquals(expectedDuration, expiry.expireAfterRead(key, successResult, 0, expectedDuration));
        Assert.assertEquals(expectedDuration, expiry.expireAfterUpdate(key, successResult, 0, expectedDuration));
    }
}
