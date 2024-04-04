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
