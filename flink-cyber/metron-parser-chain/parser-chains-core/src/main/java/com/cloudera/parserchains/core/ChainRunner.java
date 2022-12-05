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

import com.cloudera.cyber.parser.MessageToParse;

import java.util.List;

/**
 * Parses a {@link Message} using a parser chain.
 */
public interface ChainRunner {

    /**
     * Parses text input using a parser chain.
     * @param toParse The input to parse.
     * @param chain The parser chain that parses each message.
     */
    List<Message> run(String toParse, ChainLink chain);
    List<Message> run(Message toParse, ChainLink chain, List<Message> results);
    List<Message> run(MessageToParse toParse, ChainLink chain);


    /**
     * The original message that is constructed for the parser chain.
     * @param toParse The text to parse.
     */
    Message originalMessage(String toParse);
    Message originalMessage(MessageToParse toParse);
}
