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

import java.nio.charset.StandardCharsets;

@EqualsAndHashCode
public class MessageToParseFieldValue implements FieldValue {
    private final MessageToParse value;

    public static FieldValue of(MessageToParse fieldValue) {
        return new MessageToParseFieldValue(fieldValue);
    }

    /**
     * Instead use {@link MessageToParseFieldValue#of(MessageToParse)}.
     */
    private MessageToParseFieldValue(MessageToParse value) {
        this.value = value;
    }

    public String get() {
        return toString();
    }

    public byte[] toBytes() {
        return value.getOriginalBytes();
    }

    public String toString() {
        return value != null ? new String(value.getOriginalBytes(), StandardCharsets.UTF_8) : "null";
    }

    @Override
    public MessageToParse toMessageToParse() {
        return value;
    }


}
