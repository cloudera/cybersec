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

import org.apache.http.client.methods.HttpGet;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class GetRestRequest extends RestRequest {

    public GetRestRequest(RestEnrichmentConfig config) throws Exception {
        super(config);
    }

    @Nonnull
    @Override
    public CompletableFuture<RestRequestResult> asyncLoad(@Nonnull RestRequestKey key, @Nonnull Executor executor) {

        HttpGet getRequest = new HttpGet(key.getRestUri());
        return executeRequest(executor, key, getRequest);
    }

    @Override
    protected RestRequestKey getKey(Map<String, String> variables) {
        return new RestRequestKey(variables, urlTemplate);
    }

}
