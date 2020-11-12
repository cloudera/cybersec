package com.cloudera.cyber.enrichment.rest;


import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import org.apache.flink.util.Preconditions;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.PrivateKeyStrategy;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import javax.annotation.Nonnull;
import javax.net.ssl.SSLContext;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.apache.http.conn.ssl.SSLConnectionSocketFactory.STRICT_HOSTNAME_VERIFIER;

@Slf4j
public abstract class RestRequest implements AsyncCacheLoader<RestRequestKey, RestRequestResult>, Closeable {

    public static final String REST_REQUEST_RETURNS_FAILED_STATUS = "Rest request returned a success code but the content indicated failure.";
    public static final String REST_REQUEST_HTTP_FAILURE = "Rest request failed due to '%s'.";
    public static final String REST_REQUEST_ERROR_MESSAGE_WITH_KEY = "Rest request url='%s' entity='%s' failed '%s'";
    private final String resultJsonPath;
    private final String statusJsonPath;
    private final StringSubstitutor propertySubstitutor;
    private final CloseableHttpClient client;
    private final AsyncLoadingCache<RestRequestKey, RestRequestResult> cache;
    protected String urlTemplate;
    private Map<String, String> headers;


    public RestRequest(RestEnrichmentConfig config) throws Exception {
        Preconditions.checkNotNull(config, "Rest config should not be null");
        Preconditions.checkNotNull(config.getEndpointTemplate(), "Rest configuration endpoint template must be specified");
        Preconditions.checkNotNull(config.getResultsJsonPath(), "Json path for extracting results from json must be specified");
        Map<String, String> requestProperties = config.getProperties();
        if (requestProperties != null && !requestProperties.isEmpty()) {
            this.propertySubstitutor = new StringSubstitutor(config.getProperties());
        } else {
            this.propertySubstitutor = null;
        }
        this.urlTemplate = substituteProperties(config.getEndpointTemplate());
        createHeaders(config);
        this.resultJsonPath = substituteProperties(config.getResultsJsonPath());
        this.statusJsonPath = substituteProperties(config.getSuccessJsonPath());
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        setupConnectionSocketFactory(config, httpClientBuilder);
        this.client = httpClientBuilder.build();
        this.cache = Caffeine.newBuilder().maximumSize(config.getCacheSize()).
                expireAfter(new RestRequestCacheExpiry(config.getSuccessCacheExpirationSeconds(), TimeUnit.SECONDS, config.getFailureCacheExpirationSeconds(), TimeUnit.SECONDS))
                .buildAsync(this);
    }

    private static PrivateKeyStrategy getPrivateKeyStrategy(final String aliasName) {
        if (aliasName != null) {
            return (map, socket) -> aliasName;
        } else {
            return null;
        }
    }

    private static KeyStore loadKeyStore(String path, String password) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        KeyStore store = KeyStore.getInstance("JKS");
        store.load(new FileInputStream(path), password.toCharArray());

        return store;
    }

    protected String substituteProperties(String template) {
        if (propertySubstitutor != null) {
            return propertySubstitutor.replace(template);
        } else {
            return template;
        }
    }

    private void setupConnectionSocketFactory(RestEnrichmentConfig config, HttpClientBuilder builder) throws Exception {
        if (config.getTls() != null) {
            TlsConfig tlsConfig = config.getTls();
            Preconditions.checkNotNull(tlsConfig.getTrustStorePath());
            Preconditions.checkNotNull(tlsConfig.getTrustStorePassword());
            Preconditions.checkNotNull(tlsConfig.getKeyStorePath());
            Preconditions.checkNotNull(tlsConfig.getKeyStorePassword());

            SSLContextBuilder sslContextBuilder = SSLContexts.custom();

            // load the trust store
            KeyStore trustStore = loadKeyStore(tlsConfig.getTrustStorePath(), tlsConfig.getTrustStorePassword());
            sslContextBuilder = sslContextBuilder.loadTrustMaterial(trustStore);

            // load the key store
            KeyStore keyStore = loadKeyStore(tlsConfig.getKeyStorePath(), tlsConfig.getKeyStorePassword());
            sslContextBuilder = sslContextBuilder.loadKeyMaterial(keyStore, tlsConfig.getKeyPassword().toCharArray(), getPrivateKeyStrategy(tlsConfig.getKeyAlias()));

            //Building the SSLContext using the build() method
            SSLContext sslcontext = sslContextBuilder.build();

            //Creating SSLConnectionSocketFactory object
            builder.setSSLSocketFactory(new SSLConnectionSocketFactory(sslcontext, STRICT_HOSTNAME_VERIFIER));
        }
    }

    private void createHeaders(RestEnrichmentConfig config) {
        Map<String, String> configuredHeaders = config.getHeaders();
        if (configuredHeaders != null) {
            this.headers = configuredHeaders.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> substituteProperties(e.getValue())));
        }

        EndpointAuthorizationConfig authConfig = config.getAuthorization();
        if (authConfig != null) {
            this.headers.put(HttpHeaders.AUTHORIZATION, authConfig.generateAuthString(propertySubstitutor));
        }

    }

    protected void addErrorToResult(RestRequestKey key, RestRequestResult result, String errorMessage) {
        result.getErrors().add(String.format(REST_REQUEST_ERROR_MESSAGE_WITH_KEY, key.getRestUri().toASCIIString(), key.getEntity(), errorMessage));
    }

    protected CompletableFuture<RestRequestResult> executeRequest(Executor executor, RestRequestKey key, HttpUriRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            headers.forEach(request::addHeader);
            RestRequestResult result = new RestRequestResult();
            log.debug("Making rest request {}", key);
            try (CloseableHttpResponse response = client.execute(request)) {
                if ((response.getStatusLine().getStatusCode() / 100) == 2) {
                    String json = EntityUtils.toString(response.getEntity());

                    DocumentContext jsonContext = JsonPath.parse(json);
                    Boolean success = true;
                    if (statusJsonPath != null) {
                        success = jsonContext.read(statusJsonPath);
                    }
                    if (success) {
                        Map<String, Object> resultMap = jsonContext.read(resultJsonPath);
                        Map<String, String> resultExtensions = result.getExtensions();
                        resultMap.forEach((k, v) -> resultExtensions.put(k, v.toString()));
                        log.debug("Rest {} results {}", request.getURI(), result);
                    } else {
                        addErrorToResult(key, result, REST_REQUEST_RETURNS_FAILED_STATUS);
                    }
                } else {
                    addErrorToResult(key, result, String.format(REST_REQUEST_HTTP_FAILURE, response.getStatusLine().toString()));
                }
            } catch (Exception e) {
                addErrorToResult(key, result, e.getMessage());
            }
            return result;
        }, executor);
    }

    public CompletableFuture<RestRequestResult> getResult(boolean reportKeyErrors, Map<String, String> extensions) {
        RestRequestKey key = getKey(extensions);
        if (key.getErrors().isEmpty()) {
            return cache.get(key);
        } else {
            List<String> keyErrors = reportKeyErrors ? key.getErrors() : Collections.emptyList();
            return CompletableFuture.completedFuture(new RestRequestResult(Collections.emptyMap(), keyErrors));
        }
    }

    @Nonnull
    @Override
    public abstract CompletableFuture<RestRequestResult> asyncLoad(@Nonnull RestRequestKey key, @Nonnull Executor executor);

    protected abstract RestRequestKey getKey(Map<String, String> variables);

    @Override
    public void close() throws IOException {
        if (client != null) {
            client.close();
        }
    }

}
