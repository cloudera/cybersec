package com.cloudera.cyber.enrichment.rest;

import com.cloudera.cyber.enrichment.rest.impl.MockRestServer;
import org.junit.*;

import java.util.HashMap;
import java.util.Map;

public class SecurePostRestRequestTest extends PostRestRequestTest {

    private static final String KEY_ALIAS = "client";

    @BeforeClass
    public static void createMockService() {
        createMockService(true);
    }

    @AfterClass
    public static void stopMockServer() {
        mockRestServer.close();
    }

    @Test
    public void testTlsWithAlias() throws Exception {
        RestEnrichmentConfig.RestEnrichmentConfigBuilder modelResultPostRequestWithKeyAlias = mockRestServer.configureTLS(mockRestServer.configureModelPostRequest(), KEY_ALIAS);
        testSuccessfulModelPosts(modelResultPostRequestWithKeyAlias.build());
    }

    @Test
    @Ignore
    public void testTlsHandshakeException() throws Exception {
        RestEnrichmentConfig.RestEnrichmentConfigBuilder modelResultPostRequestWithKeyAlias = mockRestServer.configureTLS(mockRestServer.configureModelPostRequest(), "nosuchkeyalias");
        RestRequest badHandshakePost = new PostRestRequest(modelResultPostRequestWithKeyAlias.build());
        MockRestServer.ExpectedModelResult expectedResult = MockRestServer.expectedModelResults.get(0);
        Map<String, String> extensions = new HashMap<String, String>() {{
            put(MockRestServer.DOMAIN_EXTENSION_NAME, expectedResult.getDomainName());
        }};
        RestRequestResult result = badHandshakePost.getResult(true, extensions).get();
        verifyErrorResult(result, "Rest request url='%s://%s/model' entity='{\"accessKey\":\"mup8kz1hsl3erczwepbt8jupamita6y6\",\"request\":{\"domain\":\"google\"}}' failed 'Received fatal alert: bad_certificate'");
    }

}