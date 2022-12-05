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

import com.google.common.base.Joiner;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class RestRequestKeyTest {
    private static final String HOST_VARIABLE = "host";
    private static final String ACCESS_KEY_VARIABLE = "access_key";
    private static final String DOMAIN_VARIABLE = "domain";
    private static final String VARIABLE_FORMAT = "${%s}";
    private static final String VARIABLE_WITH_DEFAULT_FORMAT = "${%s:-%s}";

    private static final String DOMAIN_DEFAULT = "domaindefault";
    private static final String DOMAIN_WITH_DEFAULT_SUBSTITUTION = String.format(VARIABLE_WITH_DEFAULT_FORMAT, DOMAIN_VARIABLE, DOMAIN_DEFAULT);

    private static final String ACCESS_KEY_DEFAULT = "accesskeydefault";
    private static final String ACCESS_KEY_WITH_DEFAULT_SUBSTITUTION = String.format(VARIABLE_WITH_DEFAULT_FORMAT, ACCESS_KEY_VARIABLE, ACCESS_KEY_DEFAULT);

    private static final String JSON_ENTITY_FORMAT = "{\"accessKey\":\"%s\",\"request\":{\"domain\":\"%s\"}}";
    private static final String JSON_ENTITY_TEMPLATE = String.format(JSON_ENTITY_FORMAT, getTemplateVariables(ACCESS_KEY_VARIABLE, DOMAIN_VARIABLE));
    private static final String JSON_ENTITY_DEFAULT_TEMPLATE = String.format(JSON_ENTITY_FORMAT, ACCESS_KEY_WITH_DEFAULT_SUBSTITUTION, DOMAIN_WITH_DEFAULT_SUBSTITUTION);

    private static final String HOST_DEFAULT = "defaulthost";
    private static final String HOST_WITH_DEFAULT_SUBSTITUTION = String.format(VARIABLE_WITH_DEFAULT_FORMAT, HOST_VARIABLE, HOST_DEFAULT);

    private static final String REST_URL_FORMAT = "http://%s/path";
    private static final String REST_URL_TEMPLATE = String.format(REST_URL_FORMAT, getTemplateVariables(HOST_VARIABLE));
    private static final String REST_URL_DEFAULT_TEMPLATE = String.format(REST_URL_FORMAT, HOST_WITH_DEFAULT_SUBSTITUTION);

    @Test
    public void testUriOnlyNoUndefined() {
        Map<String, String> variables = new HashMap<String, String>() {{
            put(HOST_VARIABLE, "www.testhost.com");
        }};
        verifyCreateUriOnly(variables, REST_URL_TEMPLATE,  String.format(REST_URL_FORMAT, variables.get(HOST_VARIABLE)), Collections.emptyList());
    }

    @Test
    public void testUriOnlyWithSingleUndefined() {
        List<String> expectedMessages = Collections.singletonList(String.format(RestRequestKey.UNDEFINED_VARIABLE_MESSAGE, HOST_VARIABLE, RestRequestKey.URL_SOURCE));
        verifyCreateUriOnly(Collections.emptyMap(), REST_URL_TEMPLATE,  null, expectedMessages);
    }

    @Test
    public void testUriOnlyWithSomeDefinedAndMultipleUndefined() {
        Map<String, String> variables = new HashMap<String, String>() {{
            put(HOST_VARIABLE, "www.testhost.com");
        }};
        String pathVariable = "path";
        String serviceVariable = "service";
        List<String> expectedMessages = Collections.singletonList(String.format(RestRequestKey.UNDEFINED_VARIABLE_MESSAGE, Joiner.on(",").join(pathVariable, serviceVariable), RestRequestKey.URL_SOURCE));
        verifyCreateUriOnly(variables,String.format("http://${%s}/${%s}/${%s}", HOST_VARIABLE, pathVariable, serviceVariable),  null, expectedMessages);
    }

    @Test
    public void testUriOnlyWithInvalidUri() {
        String badURI = "\\$BAD/path";
        List<String> expectedMessages = Collections.singletonList(String.format(RestRequestKey.INVALID_URI_ERROR, badURI));
        verifyCreateUriOnly(Collections.emptyMap(), badURI,  null, expectedMessages);
    }

    @Test
    public void testUriWithEntityNoUndefined() {
        Map<String, String> variables = new HashMap<String, String>() {{
            put(ACCESS_KEY_VARIABLE, "access_key");
            put(DOMAIN_VARIABLE, "exampledomain");
            put(HOST_VARIABLE, "testhost.com");
        }};
        verifyCreateUriWithEntity(variables, REST_URL_TEMPLATE, JSON_ENTITY_TEMPLATE, String.format(REST_URL_FORMAT, variables.get(HOST_VARIABLE)),
                String.format(JSON_ENTITY_FORMAT, variables.get(ACCESS_KEY_VARIABLE), variables.get(DOMAIN_VARIABLE)), Collections.emptyList());
    }

    @Test
    public void testUriWithDefaultInUrlAndEntityTemplateNoUndefined() {
        // blank string variables will be reported as undefined
        Map<String, String> variables = new HashMap<String, String>() {{
            put(DOMAIN_VARIABLE, "");
            put(HOST_VARIABLE, "      ");
        }};
        verifyCreateUriWithEntity(variables, REST_URL_DEFAULT_TEMPLATE, JSON_ENTITY_DEFAULT_TEMPLATE,
               String.format(REST_URL_FORMAT, HOST_DEFAULT), String.format(JSON_ENTITY_FORMAT, ACCESS_KEY_DEFAULT, DOMAIN_DEFAULT),
               Collections.emptyList());
    }

    @Test
    public void testUriWithEntityUndefinedVariables() {
        // blank string variables will be reported as undefined
        Map<String, String> variables = new HashMap<String, String>() {{
            put(DOMAIN_VARIABLE, "");
            put(HOST_VARIABLE, "      ");
        }};
        List<String> expectedMessages = new ArrayList<>();
        expectedMessages.add(String.format(RestRequestKey.UNDEFINED_VARIABLE_MESSAGE, HOST_VARIABLE, RestRequestKey.URL_SOURCE));
        expectedMessages.add(String.format(RestRequestKey.UNDEFINED_VARIABLE_MESSAGE, Joiner.on(",").join(ACCESS_KEY_VARIABLE, DOMAIN_VARIABLE), RestRequestKey.ENTITY_SOURCE));

        verifyCreateUriWithEntity(variables, REST_URL_TEMPLATE, JSON_ENTITY_TEMPLATE, null,
                JSON_ENTITY_TEMPLATE, expectedMessages);
    }

    @Test
    public void testNonStringValues() {
        String  latitudeVar = "latitude";
        String  longitudeVar = "longitude";
        String  portVar = "port";
        String  hostVar = "host";

        Map<String, String> variables = new HashMap<String, String>() {{
            put(latitudeVar, "37.4224764");
            put(longitudeVar, "-122.0842499");
            put(portVar, "14443");
            put(hostVar, "hostname");
        }};
        String urlFormat = "http://%s:%s/path";
        String entityFormat = "{\"latitude\":\"%s\", \"latitude\":\"%s\"}";
        String urlTemplate = String.format(urlFormat, getTemplateVariables(hostVar, portVar));
        String expectedUrl = String.format(urlFormat, variables.get(hostVar), variables.get(portVar));
        String entityTemplate = String.format(entityFormat, getTemplateVariables(latitudeVar, longitudeVar));
        String expectedEntity = String.format(entityFormat, variables.get(latitudeVar), variables.get(longitudeVar));
        verifyCreateUriWithEntity(variables, urlTemplate, entityTemplate, expectedUrl, expectedEntity, Collections.emptyList());
    }

    private static Object[] getTemplateVariables(String ...variableNames) {
        Object[] templates = new String[variableNames.length];

        for(int i = 0; i < variableNames.length; i++) {
            templates[i] = String.format(VARIABLE_FORMAT, variableNames[i]);
        }
        return templates;
    }

    private void verifyCreateUriOnly(Map<String, String> variables, String urlTemplate, String expectedUrlString, List<String> expectedErrors) {
        RestRequestKey key = new RestRequestKey(variables, urlTemplate);
        Assert.assertEquals(expectedErrors, key.getErrors());
        verifyUri(key, expectedUrlString);
        Assert.assertNull(key.getEntity());
    }

    private void verifyCreateUriWithEntity(Map<String, String> variables, String urlTemplate, String entityTemplate,
                                           String expectedUrlString, String expectedEntityString, List<String> expectedErrors) {
        RestRequestKey key = new RestRequestKey(variables, urlTemplate, entityTemplate);
        Assert.assertEquals(expectedErrors, key.getErrors());
        verifyUri(key, expectedUrlString);
        Assert.assertEquals(expectedEntityString, key.getEntity());
    }

    private void verifyUri(RestRequestKey key, String expectedUrlString) {
        if (expectedUrlString != null) {
            Assert.assertEquals(expectedUrlString, key.getRestUri().toASCIIString());
        } else {
            Assert.assertNull(key.getRestUri());
        }
    }
}
