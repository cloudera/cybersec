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
import lombok.EqualsAndHashCode;
import lombok.ToString;


import static java.nio.charset.StandardCharsets.UTF_8;

@EqualsAndHashCode
public class StringFieldValue implements FieldValue {
    static final int MAX_LENGTH = 65_536;
    private final String value;

    public static FieldValue of(String fieldValue) {
        return new StringFieldValue(fieldValue);
    }

    /**
     * Instead use {@link StringFieldValue#of(String)}.
     */
    private StringFieldValue(String value) {
        if(value == null || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Invalid field value.");
        }
        this.value = value;
    }

    public String get() {
        return value != null ? value : "null";
    }

    public byte[] toBytes() {
        return value != null ? value.getBytes(UTF_8) : null;
    }

    public String toString() {
        return (value != null) ? value : "null";
    }
    @Override
    public MessageToParse toMessageToParse() {
        return MessageToParse.builder().originalBytes(toBytes()).offset(0).partition(0).topic("none").build();
    }

}
