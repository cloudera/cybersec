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

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.annotation.Nonnull;
import org.apache.flink.util.Preconditions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

public class PostRestRequest extends RestRequest {
    private final String entityTemplate;

    public PostRestRequest(RestEnrichmentConfig config) throws Exception {
        super(config);
        String configEntityTemplate = config.getEntityTemplate();
        Preconditions.checkNotNull(configEntityTemplate);
        this.entityTemplate = substituteProperties(configEntityTemplate);
    }

    @Nonnull
    @Override
    public CompletableFuture<RestRequestResult> asyncLoad(@Nonnull RestRequestKey key, @Nonnull Executor executor) {
        try {
            HttpPost postRequest = new HttpPost(key.getRestUri());
            addEntityToRequest(postRequest, key.getEntity());
            return executeRequest(executor, key, postRequest);
        } catch (UnsupportedEncodingException e) {
            RestRequestResult result = new RestRequestResult();
            addErrorToResult(key, result, e.getMessage());
            return CompletableFuture.completedFuture(result);
        }
    }

    @Override
    public RestRequestKey getKey(Map<String, String> variables) {
        return new RestRequestKey(variables, urlTemplate, entityTemplate);
    }

    protected void addEntityToRequest(@Nonnull HttpPost postRequest, @Nonnull String entityTemplate)
          throws UnsupportedEncodingException {
        postRequest.setEntity(new StringEntity(entityTemplate));
    }
}
