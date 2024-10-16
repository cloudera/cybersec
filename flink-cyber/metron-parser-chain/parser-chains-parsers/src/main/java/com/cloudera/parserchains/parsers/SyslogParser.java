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

import com.cloudera.parserchains.core.Constants;
import com.cloudera.parserchains.core.FieldName;
import com.cloudera.parserchains.core.Message;
import com.cloudera.parserchains.core.Parser;
import com.cloudera.parserchains.core.StringFieldValue;
import com.cloudera.parserchains.core.catalog.Configurable;
import com.cloudera.parserchains.core.catalog.MessageParser;
import com.github.palindromicity.syslog.SyslogParserBuilder;
import com.github.palindromicity.syslog.SyslogSpecification;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

import static java.lang.String.format;

@MessageParser(
    name="Syslog",
    description="Parses Syslog according to RFC 3164 and 5424.")
public class SyslogParser implements Parser {
    private static final String DEFAULT_SYSLOG_SPEC = "RFC_5424";
    private FieldName inputField;
    private SyslogSpecification specification;

    public SyslogParser() {
        inputField = FieldName.of(Constants.DEFAULT_INPUT_FIELD);
        specification = SyslogSpecification.valueOf(DEFAULT_SYSLOG_SPEC);
    }

    @Configurable(
            key="specification",
            label="Specification",
            description="The Syslog specification; 'RFC_5424' or 'RFC_3164'. Default value: '" + DEFAULT_SYSLOG_SPEC + "'",
            defaultValue=DEFAULT_SYSLOG_SPEC)
    public void withSpecification(String specification) {
        if(StringUtils.isNotBlank(specification)) {
            SyslogSpecification spec = SyslogSpecification.valueOf(specification);
            withSpecification(spec);
        }
    }

    public SyslogParser withSpecification(SyslogSpecification specification) {
        this.specification = Objects.requireNonNull(specification, "A valid specification is required.");
        return this;
    }

    public SyslogSpecification getSpecification() {
        return specification;
    }

    @Configurable(key="inputField",
            label="Input Field",
            description="The name of the input field to parse. Default value: '" + Constants.DEFAULT_INPUT_FIELD + "'",
            defaultValue = Constants.DEFAULT_INPUT_FIELD,
            isOutputName = true)
    public SyslogParser withInputField(String inputField) {
        if(StringUtils.isNotBlank(inputField)) {
            this.inputField = FieldName.of(inputField);
        }
        return this;
    }

    public FieldName getInputField() {
        return inputField;
    }

    @Override
    public Message parse(Message input) {
        Message.Builder output = Message.builder().withFields(input);
        if(inputField == null) {
            output.withError("Input Field has not been defined.");

        } else if(!input.getField(inputField).isPresent()) {
            output.withError(format("Message missing expected input field '%s'", inputField.toString()));

        } else {
            input.getField(inputField).ifPresent(val -> doParse(val.toString(), output));
        }
        return output.build();
    }

    private void doParse(String valueToParse, Message.Builder output) {
        try {
            new SyslogParserBuilder()
                    .forSpecification(specification)
                    .build()
                    .parseLine(valueToParse)
                    .forEach((k, v) -> output.addField(FieldName.of(k), StringFieldValue.of(v.toString())));

        } catch(Exception e) {
            output.withError(e);
        }
    }
}
