/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloudera.parserchains.queryservice.common;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ApplicationConstants {

    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    // root
    public static final String API_BASE_URL = "/api/v1";

    // parser-config endpoint constants
    public static final String PARSER_CONFIG_BASE_URL = API_BASE_URL + "/parserconfig";
    // pipeline controller constants
    public static final String PIPELINE_BASE_URL = API_BASE_URL + "/pipeline";
    // Endpoint names
    public static final String API_CHAINS = "/chains";

    public static final String API_CLUSTERS = "/clusters";
    public static final String API_JOBS = API_CLUSTERS + "/{clusterId}/jobs";
    public static final String API_INDEXING = "/indexing";
    public static final String API_PARSER_FORM_CONFIG = "/parser-form-configuration";
    public static final String API_PARSER_TYPES = "/parser-types";
    public static final String API_PARSER_TEST = "/tests";
    public static final String API_PARSER_TEST_SAMPLES = API_PARSER_TEST + "/samples";
    // URLs
    public static final String API_CHAINS_URL = PARSER_CONFIG_BASE_URL + API_CHAINS;
    public static final String API_CHAINS_CREATE_URL = API_CHAINS_URL;
    public static final String API_CHAINS_READ_URL = API_CHAINS_URL + "/{id}";
    public static final String API_CHAINS_UPDATE_URL = API_CHAINS_READ_URL;
    public static final String API_CHAINS_DELETE_URL = API_CHAINS_READ_URL;
    public static final String API_PARSER_FORM_CONFIG_URL = PARSER_CONFIG_BASE_URL + API_PARSER_FORM_CONFIG;
    public static final String API_PARSER_TYPES_URL = PARSER_CONFIG_BASE_URL + API_PARSER_TYPES;
    public static final String API_PARSER_TEST_URL = PARSER_CONFIG_BASE_URL + API_PARSER_TEST;

    //Param names
    public static final String TEST_RUN_PARAM = "testRun";
    public static final String PIPELINE_NAME_PARAM = "pipelineName";
    public static final String CHAIN_PARAM = "chain";
    public static final String ID_PARAM = "id";
    public static final String BODY_PARAM = "body";
    public static final String HEADERS_PARAM = "headers";
    public static final String STATUS_PARAM = "status";
}
