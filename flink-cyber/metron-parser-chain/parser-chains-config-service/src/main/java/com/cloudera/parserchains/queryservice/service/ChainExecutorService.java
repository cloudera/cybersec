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

package com.cloudera.parserchains.queryservice.service;

import com.cloudera.parserchains.core.ChainLink;
import com.cloudera.parserchains.queryservice.model.exec.ParserResult;

/**
 * A service that executes a parser chain on sample data.
 */
public interface ChainExecutorService {

    /**
     * Executes a parser chain by parsing a message.
     * @param chain The parser chain to execute.
     * @param textToParse The text to parse.
     * @return The result of parsing the text with the parser chain.
     */
    ParserResult execute(ChainLink chain, String textToParse);
}
