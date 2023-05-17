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
import com.cloudera.parserchains.core.FieldValue;
import com.cloudera.parserchains.core.Message;
import com.cloudera.parserchains.core.Parser;
import com.cloudera.parserchains.core.StringFieldValue;
import com.cloudera.parserchains.core.catalog.Configurable;
import com.cloudera.parserchains.core.catalog.MessageParser;
import com.cloudera.parserchains.core.catalog.Parameter;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.ReadContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.cloudera.parserchains.core.Constants.DEFAULT_INPUT_FIELD;
import static java.lang.String.format;

/**
 * Allows a JSON document to be parsed by providing one or more path expressions.
 *
 * <p>See https://github.com/json-path/JsonPath.
 */
@MessageParser(
        name = "JSON Path",
        description = "Parse JSON using JSONPath expressions.")
@Slf4j
public class JSONPathParser implements Parser {
    private FieldName inputField;
    private LinkedHashMap<FieldName, JsonPath> expressions;

    public JSONPathParser() {
        inputField = FieldName.of(DEFAULT_INPUT_FIELD);
        // using a LinkedHashMap to ensure the expressions are executed in the order they were defined
        this.expressions = new LinkedHashMap<>();
    }

    /**
     * Add a JSONPath expression that will be executed. The result of the JSONPath expression
     * is used to add or modify a field.
     * <p>Multiple expressions can be provided to create or modify multiple fields.
     *
     * @param fieldName The name of the field to create or modify.
     * @param expr      The JSONPath expression to execute.
     */
    @Configurable(key = "expr", multipleValues = true)
    public JSONPathParser expression(
            @Parameter(key = "field", label = "Field Name", description = "The field to create or modify.", isOutputName = true) String fieldName,
            @Parameter(key = "expr", label = "Path Expression", description = "The path expression.") String expr) {
        if (StringUtils.isNoneBlank(fieldName, expr)) {
            expressions.put(FieldName.of(fieldName), JsonPath.compile(expr));
        }
        return this;
    }

    @Override
    public Message parse(Message input) {
        Message.Builder output = Message.builder().withFields(input);
        if (!input.getField(inputField).isPresent()) {
            output.withError(format("Message missing expected input field '%s'", inputField.toString()));
        } else {
            input.getField(inputField).ifPresent(val -> doParse(val.toString(), output));
        }
        return output.build();
    }

    public void doParse(String jsonToParse, Message.Builder output) {
        try {
            // parse the document only once
            ReadContext readContext = JsonPath.parse(jsonToParse);

            // handle each path expression
            for (Map.Entry<FieldName, JsonPath> entry : expressions.entrySet()) {
                FieldName fieldName = entry.getKey();
                JsonPath jsonPath = entry.getValue();

                // create the field
                FieldValue fieldValue = execute(jsonPath, readContext);
                output.addField(fieldName, fieldValue);
            }

        } catch (InvalidJsonException e) {
            output.withError(e);
        }
    }

    private FieldValue execute(JsonPath jsonPath, ReadContext readContext) {
        try {
            // execute the path expression
            Object rawValue = readContext.read(jsonPath);

            if (rawValue instanceof Collection) {
                return toStringFieldValue((Collection<?>) rawValue, readContext);
            } else if (rawValue instanceof Map) {
                return toStringFieldValue((Map<?, ?>) rawValue, readContext);
            } else {
                return StringFieldValue.of(String.valueOf(rawValue));
            }
        } catch (PathNotFoundException e) {
            log.debug("No results for path expression. Nothing to do. jsonPath={}", jsonPath.getPath());
        }
        return StringFieldValue.of("");
    }

    private FieldValue toStringFieldValue(Map<?, ?> map, ReadContext readContext) {
        if (MapUtils.isEmpty(map)) {
            return StringFieldValue.of("");
        }
        return StringFieldValue.of(readContext.configuration().jsonProvider().toJson(map));
    }

    private FieldValue toStringFieldValue(Collection<?> collection, ReadContext readContext) {
        ArrayList<?> arrayList = new ArrayList<>(collection);
        if (CollectionUtils.isEmpty(arrayList)) {
            return StringFieldValue.of("");
        } else if (arrayList.size() == 1) {
            return StringFieldValue.of(String.valueOf(arrayList.get(0)));
        } else {
            return StringFieldValue.of(readContext.configuration().jsonProvider().toJson(arrayList));
        }
    }
}
