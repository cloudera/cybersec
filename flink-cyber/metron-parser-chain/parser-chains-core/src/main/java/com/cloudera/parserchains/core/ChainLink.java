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

import java.util.List;

/**
 * One link in a parser chain.
 */
public interface ChainLink {

    /**
     * Parses an message starting at this link in the chain.
     *
     * <p>This involves all of the downstream links in this chain, not just this chain link.
     *
     * @param input The input message to parse.
     * @return One {@link Message} for every link in the parser chain.
     */
    List<Message> process(Message input);

    /**
     * Define the next link in the chain.
     *
     * <p>If not defined, this is the last link in the parser chain.
     *
     * @param nextLink The next chain link.
     */
    void setNext(ChainLink nextLink);
}
