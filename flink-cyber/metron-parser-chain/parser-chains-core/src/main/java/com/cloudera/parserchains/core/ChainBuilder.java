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

package com.cloudera.parserchains.core;

import com.cloudera.parserchains.core.model.define.ParserChainSchema;

/**
 * Builds a parser chain from the definition of a parser chain; a {@link ParserChainSchema}.
 */
public interface ChainBuilder {

    /**
     * Builds a parser chain from a {@link ParserChainSchema} which defines
     * what a parser chain looks like.
     * @param chainSchema The blueprint for building the parser chain.
     * @return The first link in the parser chain.
     * @throws InvalidParserException If the user has defined an invalid parser chain.
     */
    ChainLink build(ParserChainSchema chainSchema) throws InvalidParserException;
}
