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

package com.cloudera.cyber.enrichment.rest.impl;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.JsonBody.json;

import com.cloudera.cyber.enrichment.rest.BasicAuthorizationConfig;
import com.cloudera.cyber.enrichment.rest.BearerTokenAuthorizationConfig;
import com.cloudera.cyber.enrichment.rest.RestEnrichmentConfig;
import com.cloudera.cyber.enrichment.rest.RestEnrichmentMethod;
import com.cloudera.cyber.enrichment.rest.TlsConfig;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.AuthSchemes;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.logging.MockServerLogger;
import org.mockserver.matchers.MatchType;
import org.mockserver.model.MediaType;
import org.mockserver.socket.PortFactory;
import org.mockserver.socket.tls.KeyStoreFactory;

@Data
public class MockRestServer {

    // mock get service query parameters
    public static final String USER_NAME = "chris";
    public static final String DEPARTMENT_NAME_PROPERTY = "department";
    public static final String DEPARTMENT_NAME = "engineering";
    public static final String ASSET_ID_FIELD_NAME = "id";
    public static final String ASSET_ID = "1234";
    public static final String SERVER_ERROR_ASSET_ID = "56";
    public static final String ASSET_LOCATION_PROPERTY = "location";
    public static final String ASSET_LOCATION = "Boston, MA";
    public static final String DNS_SOURCE = "dns";
    public static final String ASSET_PREFIX = "asset_info";
    public static final String ASSET_SOURCE = "asset";
    public static final String USER_SOURCE = "user";
    public static final String USER_PREFIX = "user_info";
    public static final String DOMAIN_EXTENSION_NAME = "domain";
    public static final String LEGIT_RESPONSE = "legit";
    public static final String LEGIT_DOMAIN = "google";
    public static final String DGA_DOMAIN = "dfjadfjaf";
    public static final String UNSUCCESSFUL_DOMAIN = "unsuccessfuldomain";
    public static final String CLIENT_ERROR_DOMAIN = "clienterror";
    public static final String TEST_SOURCE = "test_source";
    public static final String TEST_PREFIX = "test_prefix";
    public static final String DGA_MODEL_PREFIX = "dga_model";
    public static final List<ExpectedModelResult> expectedModelResults = new ArrayList<ExpectedModelResult>() {{
        add(new ExpectedModelResult(LEGIT_DOMAIN, true, true));
        add(new ExpectedModelResult(DGA_DOMAIN, true, false));
        add(new ExpectedModelResult(UNSUCCESSFUL_DOMAIN, false, true));
    }};
    // SSL configuration
    protected static final String CONFIG_DIR = "src/test/resources/config/cert/";
    protected static final String BASIC_USER_NAME = "basic_user";
    protected static final String BASIC_PASSWORD = "basic_password";
    private static final String TRUST_STORE_PASSWORD = "password";
    private static final String TRUST_STORE_PATH = CONFIG_DIR + "test-truststore.jks";
    private static final String KEY_STORE_PASSWORD = "password";
    private static final String KEY_PASSWORD = "keypass";
    private static final String KEY_STORE_PATH = CONFIG_DIR + "test-client.jks";
    private static final String ACCESS_KEY = "mup8kz1hsl3erczwepbt8jupamita6y6";
    private static final String BEARER_TOKEN = "mybearertokenaae6b840d045b574d96e35e271419720d0d7645534da6d5ba3d.74c9e8867ef7e0750b5772671acf7e413a744f6d77507eac83584014c71c5866";
    private static final int EXPECTED_CLIENT_ERROR_HTTP_CODE = 400;
    public static final String USER_NAME_PROPERTY = "name";
    private ClientAndServer mockServer;
    private String mockHostAndPort;
    private String mockProtocol;
    public MockRestServer(boolean enableTlsMutualAuth) {
        if (enableTlsMutualAuth) {
            ConfigurationProperties.tlsMutualAuthenticationRequired(true);
            ConfigurationProperties.tlsMutualAuthenticationCertificateChain("src/test/resources/config/cert/test-cert.crt");
        }
        HttpsURLConnection.setDefaultSSLSocketFactory(new KeyStoreFactory(new MockServerLogger()).sslContext().getSocketFactory());
        mockServer = startClientAndServer(PortFactory.findFreePort());
        mockServer.withSecure(enableTlsMutualAuth);
        mockProtocol = enableTlsMutualAuth ? "https" : "http";
        mockHostAndPort = String.join(":", "127.0.0.1",
                Integer.toString(mockServer.getPort()));
        mockServer.reset();

        initializeGetExpectations();
        initializePostExpectations();
    }

    public RestEnrichmentConfig.RestEnrichmentConfigBuilder getBuilder(HashMap<String, String> properties) {
        RestEnrichmentConfig.RestEnrichmentConfigBuilder builder = RestEnrichmentConfig.builder()
                .capacity(3);
        boolean mutualTlsAuth = mockServer.isSecure();
        if (mutualTlsAuth) {
            configureTLS(builder, null);
        }
        if (properties != null) {
            properties.put("server", mockHostAndPort);
            properties.put("protocol", mockProtocol);
            builder.properties(properties);
        }

        return builder;
    }

    public RestEnrichmentConfig.RestEnrichmentConfigBuilder configureTLS(RestEnrichmentConfig.RestEnrichmentConfigBuilder configBuilder, String keyAlias) {
        return configBuilder
                .tls(TlsConfig.builder()
                .trustStorePath(TRUST_STORE_PATH)
                .trustStorePassword(TRUST_STORE_PASSWORD)
                .keyStorePath(KEY_STORE_PATH)
                .keyStorePassword(KEY_STORE_PASSWORD)
                .keyPassword(KEY_PASSWORD).keyAlias(keyAlias).build());
    }

    public RestEnrichmentConfig.RestEnrichmentConfigBuilder configureModelPostRequest() {
        HashMap<String, String> authTokenProperties = new HashMap<String, String>() {{
            put("bearer_token", BEARER_TOKEN);
            put("access_key", ACCESS_KEY);
        }};
        return getBuilder(authTokenProperties)
                .sources(Lists.newArrayList(DNS_SOURCE))
                .prefix(DGA_MODEL_PREFIX)
                .endpointTemplate("${protocol}://${server}/model")
                .method(RestEnrichmentMethod.POST)
                .headers(new HashMap<String, String>() {{
                    put(HttpHeaders.CONTENT_TYPE, "application/json");
                }})
                .authorization(BearerTokenAuthorizationConfig.builder().bearerTokenTemplate("${bearer_token}").build())
                .entityTemplate("{\"accessKey\":\"${access_key}\",\"request\":{\"domain\":\"${domain}\"}}")
                .successJsonPath("$['success']")
                .resultsJsonPath("$['response']");

    }

    public RestEnrichmentConfig.RestEnrichmentConfigBuilder configureGetUserRequest() {
        return getBuilder(new HashMap<>())
                .sources(Lists.newArrayList(USER_SOURCE))
                .prefix(USER_PREFIX)
                .endpointTemplate("${protocol}://${server}/user?name=${name}");
    }

    public RestEnrichmentConfig.RestEnrichmentConfigBuilder configureGetAssetRequest() {
        HashMap<String, String> properties = new HashMap<String, String>() {{
            put("user", BASIC_USER_NAME);
            put("password", BASIC_PASSWORD);
        }};

        return getBuilder(properties)
                .sources(Lists.newArrayList(ASSET_SOURCE))
                .prefix(ASSET_PREFIX)
                .endpointTemplate("${protocol}://${server}/asset?id=${id}")
                .authorization(BasicAuthorizationConfig.builder()
                        .userNameTemplate("${user}")
                        .passwordTemplate("${password}")
                        .build());
    }

    private void initializeGetExpectations() {
        mockServer.when(
                request()
                        .withMethod("GET")
                        .withPath("/user")
                        .withQueryStringParameter(USER_NAME_PROPERTY, USER_NAME)
        )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(String.format("{%s: '%s'}", DEPARTMENT_NAME_PROPERTY, DEPARTMENT_NAME))
                );

        mockServer.when(
                request()
                        .withMethod("GET")
                        .withPath("/asset")
                        .withHeader(HttpHeaders.AUTHORIZATION, createBasicAuth())
                        .withQueryStringParameter("id", ASSET_ID)
        )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(String.format("{%s: '%s'}", ASSET_LOCATION_PROPERTY, ASSET_LOCATION))
                );
        mockServer.when(
                request()
                        .withMethod("GET")
                        .withPath("/asset")
                        .withHeader(HttpHeaders.AUTHORIZATION, createBasicAuth())
                        .withQueryStringParameter("id", SERVER_ERROR_ASSET_ID)
        )
                .respond(
                        response()
                                .withStatusCode(503)
                );
    }

    private String createBasicAuth() {
        byte[] credentials = Base64.encodeBase64(String.join(":", BASIC_USER_NAME,
                BASIC_PASSWORD).getBytes(StandardCharsets.ISO_8859_1));
        return String.join(" ", AuthSchemes.BASIC, new String(credentials));
    }

    private void initializePostExpectations() {
        for (ExpectedModelResult expectedModelResult : expectedModelResults) {
            addModelResultExpectation(expectedModelResult.isSuccess(), expectedModelResult.getDomainName(), expectedModelResult.isLegit());
        }
        addModelResultClientErrorExpectation(CLIENT_ERROR_DOMAIN, EXPECTED_CLIENT_ERROR_HTTP_CODE);
    }

    private void addModelResultExpectation(boolean success, String domain, boolean legit) {
        mockServer.when(
                request()
                        .withMethod("POST")
                        .withPath("/model")
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withHeader(HttpHeaders.AUTHORIZATION, "Bearer ".concat(BEARER_TOKEN))
                        .withBody(json(String.format("{\"accessKey\":\"%s\",\"request\":{\"domain\":\"%s\"}}", ACCESS_KEY, domain), MatchType.STRICT))
        )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(String.format("{\"success\":%b,\"response\":{\"legit\":%b}}", success, legit))
                );
    }

    private void addModelResultClientErrorExpectation(String domain, int statusCode) {
        mockServer.when(
                request()
                        .withMethod("POST")
                        .withPath("/model")
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withHeader(HttpHeaders.AUTHORIZATION, "Bearer ".concat(BEARER_TOKEN))
                        .withBody(json(String.format("{\"accessKey\":\"%s\",\"request\":{\"domain\":\"%s\"}}", ACCESS_KEY, domain), MatchType.STRICT))
        )
                .respond(
                        response()
                                .withStatusCode(statusCode)
                );
    }

    public void close() {
        mockServer.stop();
    }

    @Data
    @AllArgsConstructor
    public static class ExpectedModelResult {
        private String domainName;
        private boolean success;
        private boolean legit;

        public Map<String, String> getExpectedExtensions(String prefix) {
            Map<String, String> expectedExtensions = new HashMap<>();
            expectedExtensions.put(DOMAIN_EXTENSION_NAME, domainName);
            if (success) {
                expectedExtensions.put(Joiner.on(".").skipNulls().join(prefix, LEGIT_RESPONSE), Boolean.toString(legit));
            }

            return expectedExtensions;
        }
    }
}
