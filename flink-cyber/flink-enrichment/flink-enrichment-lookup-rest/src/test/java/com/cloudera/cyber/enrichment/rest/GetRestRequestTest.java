package com.cloudera.cyber.enrichment.rest;

import com.cloudera.cyber.enrichment.rest.impl.MockRestServer;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

@Slf4j
// tests will run either with or without tls when junit runs derived classes
@Ignore
public class GetRestRequestTest extends RestRequestTest {

    public static void createMockService(boolean enableTlsMutualAuth) {
        startMockServer(enableTlsMutualAuth);
    }

    @Test
    public void testSimpleGet() throws Exception {
        RestEnrichmentConfig config = mockRestServer.configureGetUserRequest().build();

        Map<String, String> variables = new HashMap<String, String>() {{
            put("name", "Chris");
        }};
        RestRequestResult result = makeRequest(config, variables);
        Assert.assertEquals(MockRestServer.DEPARTMENT_NAME, result.getExtensions().get(MockRestServer.DEPARTMENT_NAME_PROPERTY));
    }

    @Test
    public void testPropertiesNull() throws Exception {
        RestEnrichmentConfig config = mockRestServer.getBuilder(null).sources(Lists.newArrayList(MockRestServer.USER_SOURCE)).
                endpointTemplate(String.format("%s://%s/user?name=${name}", mockRestServer.getMockProtocol(), mockRestServer.getMockHostAndPort())).build();

        Map<String, String> variables = new HashMap<String, String>() {{
            put("name", "Chris");
        }};

        RestRequestResult result = makeRequest(config, variables);
        Assert.assertEquals(MockRestServer.DEPARTMENT_NAME, result.getExtensions().get(MockRestServer.DEPARTMENT_NAME_PROPERTY));
    }

    @Test
    public void testBasicAuthGet() throws Exception {
        RestRequestResult result = makeAssetRequest(MockRestServer.ASSET_ID);
        Assert.assertEquals(MockRestServer.ASSET_LOCATION, result.getExtensions().get(MockRestServer.ASSET_LOCATION_PROPERTY));
    }

    @Test
    public void testServerError() throws Exception {
        RestRequestResult result = makeAssetRequest(MockRestServer.SERVER_ERROR_ASSET_ID);
        verifyErrorResult(result,  "Rest request url='%s://%s/asset?id=56' entity='null' failed 'Rest request failed due to 'HTTP/1.1 503 Service Unavailable'.'");
    }

    private RestRequestResult makeAssetRequest(String assetId) throws Exception {
        RestEnrichmentConfig config = mockRestServer.configureGetAssetRequest().build();
        Map<String, String> variables = new HashMap<String, String>() {{
            put("id", assetId);
        }};

        return makeRequest(config, variables);
    }

}
