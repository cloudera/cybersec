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

import static java.lang.String.format;
import com.cloudera.parserchains.core.Constants;
import com.cloudera.parserchains.core.FieldName;
import com.cloudera.parserchains.core.Message;
import com.cloudera.parserchains.core.Parser;
import com.cloudera.parserchains.core.catalog.Configurable;
import com.cloudera.parserchains.core.catalog.MessageParser;
import com.cloudera.parserchains.core.catalog.Parameter;
import com.cloudera.parserchains.core.catalog.WidgetType;
import io.krakens.grok.api.Grok;
import io.krakens.grok.api.GrokCompiler;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

@MessageParser(
        name = "Grok",
        description = "Parses a message using Grok expressions."
)
public class GrokParser implements Parser {
    private static final String DEFAULT_ZONE_OFFSET = "+00:00";
    private FieldName inputField;
    private ZoneOffset zoneOffset;
    private GrokCompiler grokCompiler;
    private List<Grok> grokExpressions;

    public GrokParser() {
        inputField = FieldName.of(Constants.DEFAULT_INPUT_FIELD);
        zoneOffset = ZoneOffset.of(DEFAULT_ZONE_OFFSET);
        grokCompiler = GrokCompiler.newInstance();
        grokCompiler.registerDefaultPatterns();
        grokExpressions = new ArrayList<>();
    }

    @Override
    public Message parse(Message input) {
        Message.Builder output = Message.builder().withFields(input);
        if (inputField == null) {
            output.withError("Input Field has not been defined.");
        } else if (!input.getField(inputField).isPresent()) {
            output.withError(format("Missing expected input field '%s'", inputField.toString()));
        } else {
            input.getField(inputField).ifPresent(val -> doParse(val.toString(), output));
        }
        return output.build();
    }

    private void doParse(String textToParse, Message.Builder output) {
        for (Grok grokPattern : grokExpressions) {
            grokPattern.match(textToParse)
                    .capture()
                    .entrySet()
                    .stream()
                    .filter(e -> e.getKey() != null && e.getValue() != null)
                    .forEach(e -> output.addField(e.getKey(), e.getValue().toString()));
        }
    }

    @Configurable(key = "grokPattern",
            description = "Define a Grok pattern that can be referenced from an expression.",
            orderPriority = 1)
    public GrokParser pattern(
            @Parameter(key = "name", label = "Pattern Name") String patternName,
            @Parameter(key = "regex", label = "Pattern Regex", widgetType = WidgetType.TEXTAREA) String patternRegex) {
        if (StringUtils.isNoneBlank(patternName, patternRegex)) {
            grokCompiler.register(patternName, patternRegex);
        }
        return this;
    }

    @Configurable(key = "grokExpression",
            label = "Grok Expression(s)",
            description = "The grok expression to execute.",
            multipleValues = true)
    public GrokParser expression(String grokExpression) {
        if (StringUtils.isNotBlank(grokExpression)) {
            Grok grok = grokCompiler.compile(grokExpression, zoneOffset, false);
            this.grokExpressions.add(grok);
        }
        return this;
    }

    public List<Grok> getGrokExpressions() {
        return Collections.unmodifiableList(grokExpressions);
    }

    public GrokParser inputField(FieldName inputField) {
        this.inputField = inputField;
        return this;
    }

    @Configurable(key = "inputField",
            label = "Input Field",
            description = "The name of the input field to parse. Default value: '" + Constants.DEFAULT_INPUT_FIELD + "'",
            defaultValue = Constants.DEFAULT_INPUT_FIELD)
    public GrokParser inputField(String inputField) {
        if (StringUtils.isNotBlank(inputField)) {
            this.inputField = FieldName.of(inputField);
        }
        return this;
    }

    public FieldName getInputField() {
        return inputField;
    }

    public GrokParser zoneOffset(ZoneOffset zoneOffset) {
        this.zoneOffset = zoneOffset;
        return this;
    }

    @Configurable(key = "zoneOffset",
            label = "Zone Offset",
            description = "Set the zone offset. For example \"+02:00\". Default value: '" + DEFAULT_ZONE_OFFSET + "'",
            defaultValue = DEFAULT_ZONE_OFFSET)
    public void zoneOffset(String offset) {
        if (StringUtils.isNotBlank(offset)) {
            zoneOffset(ZoneOffset.of(offset));
        }
    }

    public ZoneOffset getZoneOffset() {
        return zoneOffset;
    }
}
