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

package com.cloudera.parserchains.parsers;

import com.cloudera.parserchains.core.FieldName;
import com.cloudera.parserchains.core.Message;
import com.cloudera.parserchains.core.Parser;
import com.cloudera.parserchains.core.catalog.Configurable;
import com.cloudera.parserchains.core.catalog.MessageParser;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * A parser which can remove fields from a message.
 */
@MessageParser(
    name="Remove Field(s)",
    description="Removes unwanted message field(s).")
public class RemoveFieldParser implements Parser {
    private List<FieldName> fieldsToRemove;

    public RemoveFieldParser() {
        fieldsToRemove = new ArrayList<>();
    }

    public RemoveFieldParser removeField(FieldName fieldName) {
        fieldsToRemove.add(fieldName);
        return this;
    }

    @Override
    public Message parse(Message message) {
        return Message.builder()
                .withFields(message)
                .removeFields(fieldsToRemove)
                .build();
    }

    List<FieldName> getFieldsToRemove() {
        return fieldsToRemove;
    }

    @Configurable(
            key="fieldToRemove",
            label="Field to Remove",
            description="The name of a field to remove.",
            multipleValues=true,
            required=true)
    public void removeField(String fieldName) {
        if(StringUtils.isNotBlank(fieldName)) {
            removeField(FieldName.of(fieldName));
        }
    }
}
