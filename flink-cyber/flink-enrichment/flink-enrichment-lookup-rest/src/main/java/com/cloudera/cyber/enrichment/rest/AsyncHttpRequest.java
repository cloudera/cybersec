package com.cloudera.cyber.enrichment.rest;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageUtils;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.type.TypeReference;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;

@Slf4j
public class AsyncHttpRequest extends RichAsyncFunction<Message, Message> {

    private final @NonNull RestEnrichmentConfig config;

    private final transient CloseableHttpClient client;

    private transient AsyncLoadingCache<String, Map<String, String>> cache;

    private transient ObjectMapper om = new ObjectMapper();

    public AsyncHttpRequest(@NonNull RestEnrichmentConfig config) {
        this.config = config;

        // setup the client
        this.client = HttpClientBuilder.create()
                // add proxy and auth support
                .build();

        this.cache = Caffeine.newBuilder().maximumSize(config.getCacheSize())
                .buildAsync((u) -> {
                    HttpGet httpget = new HttpGet(u);
                    try {
                        CloseableHttpResponse execute = client.execute(httpget);
                        Map<String, String> results = om.readValue(
                                execute.getEntity().getContent(),
                                new TypeReference<Map<String, String>>() {
                                });
                        return results;
                    } catch (IOException e) {
                        // TODO - add error handling
                        log.error("HTTP request failed", e);
                        return null;
                    }
                });
    }

    @Override
    public void open(Configuration parameters) throws Exception {

    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public void asyncInvoke(Message message, ResultFuture<Message> resultFuture) throws Exception {
        // issue the asynchronous request, receive a future for result
        StringSubstitutor sub = new StringSubstitutor(message.getExtensions());
        String url = sub.replace(config.getEndpointTemplate());

        log.debug(String.format("Fetching enrichment from url %s", url));
        final Future<Map<String, String>> result = this.cache.get(url);

        // set the callback to be executed once the request by the client is complete
        // the callback simply forwards the result to the result future
        CompletableFuture.supplyAsync(new Supplier<Map<String, String>>() {
            @Override
            public Map<String, String> get() {
                try {
                    return result.get();
                } catch (InterruptedException | ExecutionException e) {
                    // Normally handled explicitly.
                    return null;
                }
            }
        }).thenAccept((Map<String, String> fields) -> {
            resultFuture.complete(Collections.singleton(MessageUtils.addFields(message, fields, config.getPrefix() + ".")));
        });
    }
}
