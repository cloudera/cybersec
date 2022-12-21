/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloudera.parserchains.queryservice.model.exec;

import com.cloudera.parserchains.core.model.define.ParserChainSchema;
import com.cyber.jackson.annotation.JsonProperty;

/**
 * Defines the data model for a request to test a parser chain.
 *
 * <p>Describes the parser chain along with the message that should
 * be parsed.
 *
 * <p>This is the top-level class of the request that is made for
 * the "Live View" feature.
 *
 * <p>See also {@link ChainTestResponse}.
 */
public class ChainTestRequest {

    /**
     * The sample data that needs to be parsed.
     */
    @JsonProperty("sampleData")
    private SampleData sampleData;

    /**
     * Defines the parser chain that needs to be constructed.
     */
    @JsonProperty("chainConfig")
    private ParserChainSchema parserChainSchema;

    public SampleData getSampleData() {
        return sampleData;
    }

    public ChainTestRequest setSampleData(SampleData sampleData) {
        this.sampleData = sampleData;
        return this;
    }

    public ParserChainSchema getParserChainSchema() {
        return parserChainSchema;
    }

    public ChainTestRequest setParserChainSchema(ParserChainSchema parserChainSchema) {
        this.parserChainSchema = parserChainSchema;
        return this;
    }
}
