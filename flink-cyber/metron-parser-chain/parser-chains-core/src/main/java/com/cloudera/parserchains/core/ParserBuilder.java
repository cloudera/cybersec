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

import com.cloudera.parserchains.core.catalog.ParserInfo;
import com.cloudera.parserchains.core.model.define.ParserSchema;

/**
 * Constructs a {@link Parser}.
 */
public interface ParserBuilder {

    /**
     * Constructs a {@link Parser} instance given the {@link ParserInfo} retrieved
     * from a {@link com.cloudera.parserchains.core.catalog.ParserCatalog}.
     * @param parserInfo Describes the parser to build.
     * @param parserSchema Describes how the parser should be configured.
     * @return
     */
    Parser build(ParserInfo parserInfo, ParserSchema parserSchema) throws InvalidParserException;
}
