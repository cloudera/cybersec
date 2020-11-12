package com.cloudera.cyber.enrichment.rest;

import com.cloudera.cyber.enrichment.rest.impl.MockRestServer;
import org.junit.Assert;

import java.util.List;
import java.util.Map;

public abstract class RestRequestTest {
    protected static MockRestServer mockRestServer;


    protected static void startMockServer(boolean enableTlsMutualAuth) {
        mockRestServer = new MockRestServer(enableTlsMutualAuth);
    }

    protected RestRequestResult makeRequest(RestEnrichmentConfig config, Map<String, String> variables) throws Exception {
        try (RestRequest request = config.createRestEnrichmentRequest()) {
            return request.getResult(!config.getSources().contains(AsyncHttpRequest.ANY_SOURCE_NAME), variables).get();
        }
    }

    protected void verifyErrorResult(RestRequestResult result, String expectedErrorFormat) {
        Assert.assertTrue(result.getExtensions().isEmpty());
        List<String> errors = result.getErrors();
        Assert.assertEquals(1, errors.size());
        Assert.assertEquals(String.format(expectedErrorFormat, mockRestServer.getMockProtocol(), mockRestServer.getMockHostAndPort()), errors.get(0));
    }

}
