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

import com.cloudera.parserchains.core.model.define.ParserSchema;

/**
 * An exception that occurs when the user has defined a parser that is invalid
 * and is impossible to construct.
 */
public class InvalidParserException extends Exception {
    private final ParserSchema badParser;

    /**
     * InvalidParserException constructor.
     *
     * @param badParser The parser that caused this error.
     * @param cause     The root cause exception.
     */
    public InvalidParserException(ParserSchema badParser, Throwable cause) {
        super(cause);
        this.badParser = badParser;
    }

    /**
     * InvalidParserException constructor.
     *
     * @param badParser The parser that caused this error.
     * @param message   The error message.
     */
    public InvalidParserException(ParserSchema badParser, String message) {
        super(message);
        this.badParser = badParser;
    }

    /**
     * InvalidParserException constructor.
     *
     * @param badParser The parser that caused this error.
     * @param message   The error message.
     * @param cause     The root cause exception.
     */
    public InvalidParserException(ParserSchema badParser, String message, Throwable cause) {
        super(message, cause);
        this.badParser = badParser;
    }

    public ParserSchema getBadParser() {
        return badParser;
    }
}
