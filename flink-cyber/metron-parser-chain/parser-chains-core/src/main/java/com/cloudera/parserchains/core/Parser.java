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

/**
 * Parses a {@link Message}.
 */
public interface Parser {

    /**
     * Parse a {@link Message}.
     *
     * <p>Parsers should not throw exceptions to indicate failure to parse conditions. Instead,
     * use {@link Message.Builder#withError}.
     *
     * @param message The message to parse.
     * @return A parsed message.
     */
    Message parse(Message message);
}
