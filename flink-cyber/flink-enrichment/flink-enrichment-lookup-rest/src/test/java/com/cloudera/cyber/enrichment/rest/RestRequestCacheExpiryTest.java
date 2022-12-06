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
