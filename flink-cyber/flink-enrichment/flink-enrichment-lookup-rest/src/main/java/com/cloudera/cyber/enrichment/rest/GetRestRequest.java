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
