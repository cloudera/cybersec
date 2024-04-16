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

package com.cloudera.parserchains.queryservice.service.impl;

import com.cloudera.parserchains.core.ChainBuilder;
import com.cloudera.parserchains.core.ChainLink;
import com.cloudera.parserchains.core.InvalidParserException;
import com.cloudera.parserchains.core.model.define.ParserChainSchema;
import com.cloudera.parserchains.queryservice.service.ChainBuilderService;
import org.springframework.stereotype.Service;

@Service
public class DefaultChainBuilderService implements ChainBuilderService {
    private ChainBuilder chainBuilder;

    public DefaultChainBuilderService(ChainBuilder chainBuilder) {
        this.chainBuilder = chainBuilder;
    }

    @Override
    public ChainLink build(ParserChainSchema chainSchema) throws InvalidParserException {
        return chainBuilder.build(chainSchema);
    }
}
