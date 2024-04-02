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

import com.cloudera.cyber.enrichment.rest.impl.MockRestServer;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class GetRestRequestTest extends RestRequestTest {

    @BeforeClass
    public static void createMockServices() {
        startMockServer();
    }

    @AfterClass
    public static void stopMockServices() {
        stopMockServer();
    }

    @Test
    public void testSimpleGet() throws Exception {
        RestEnrichmentConfig config = mockRestServer.configureGetUserRequest(MockRestServer.ENCRYPTED_PROTOCOL).build();

        Map<String, String> variables = new HashMap<String, String>() {{
            put("name", "Chris");
        }};
        RestRequestResult result = makeRequest(config, variables);
        Assert.assertEquals(MockRestServer.DEPARTMENT_NAME, result.getExtensions().get(MockRestServer.DEPARTMENT_NAME_PROPERTY));
    }

    @Test
    public void testPropertiesNull() throws Exception {
        RestEnrichmentConfig config = mockRestServer.getBuilder(null, MockRestServer.ENCRYPTED_PROTOCOL).sources(Lists.newArrayList(MockRestServer.USER_SOURCE)).
                endpointTemplate(String.format("%s://%s/user?name=${name}", mockRestServer.getMockProtocol(), mockRestServer.getMockHostAndPort())).build();

        Map<String, String> variables = new HashMap<String, String>() {{
            put("name", "Chris");
        }};

        RestRequestResult result = makeRequest(config, variables);
        Assert.assertEquals(MockRestServer.DEPARTMENT_NAME, result.getExtensions().get(MockRestServer.DEPARTMENT_NAME_PROPERTY));
    }

    @Test
    public void testBasicAuthGet() throws Exception {
        RestRequestResult result = makeAssetRequest(MockRestServer.ASSET_ID, mockRestServer);
        Assert.assertEquals(MockRestServer.ASSET_LOCATION, result.getExtensions().get(MockRestServer.ASSET_LOCATION_PROPERTY));
    }

    @Test
    public void testServerError() throws Exception {
        RestRequestResult result = makeAssetRequest(MockRestServer.SERVER_ERROR_ASSET_ID, mockRestServer);
        verifyErrorResult(result,  "Rest request url='%s://%s/asset?id=56' entity='null' failed 'Rest request failed due to 'HTTP/1.1 503 Service Unavailable'.'", mockRestServer);
    }

    private RestRequestResult makeAssetRequest(String assetId, MockRestServer mockRestServer) throws Exception {
        RestEnrichmentConfig config = mockRestServer.configureGetAssetRequest(MockRestServer.ENCRYPTED_PROTOCOL).build();
        Map<String, String> variables = new HashMap<String, String>() {{
            put("id", assetId);
        }};

        return makeRequest(config, variables);
    }

}
