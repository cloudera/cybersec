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
import com.cloudera.parserchains.core.catalog.Parameter;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A parser that can rename message fields.
 */
@MessageParser(
    name="Rename Field(s)", 
    description="Renames message field(s).")
public class RenameFieldParser implements Parser {
    private Map<FieldName, FieldName> fieldsToRename;

    public RenameFieldParser() {
        this.fieldsToRename = new HashMap<>();
    }

    /**
     * Configure the parser to rename a field.
     * @param from The original field name.
     * @param to The new field name.
     */
    public RenameFieldParser renameField(FieldName from, FieldName to) {
        fieldsToRename.put(from, to);
        return this;
    }

    Map<FieldName, FieldName> getFieldsToRename() {
        return Collections.unmodifiableMap(fieldsToRename);
    }

    @Override
    public Message parse(Message input) {
        Message.Builder output = Message.builder()
                .withFields(input);
        fieldsToRename.forEach((from, to) -> output.renameField(from, to));
        return output.build();
    }

    @Configurable(
            key="fieldToRename",
            multipleValues=true)
    public void renameField(
            @Parameter(key="from", label="Rename From", description="The original name of the field.") String from,
            @Parameter(key="to", label="Rename To", description="The new name of the field.") String to) {
        if(StringUtils.isNoneBlank(from, to)) {
            renameField(FieldName.of(from), FieldName.of(to));
        }
    }
}
