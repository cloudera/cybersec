package com.cloudera.parserchains.parsers;

import com.cloudera.parserchains.core.*;
import com.cloudera.parserchains.core.catalog.Configurable;
import com.cloudera.parserchains.core.catalog.MessageParser;
import com.cloudera.parserchains.core.catalog.Parameter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.ReadContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private ObjectMapper objectMapper = new ObjectMapper();

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
    @Configurable(key = "expr")
    public JSONPathParser expression(
            @Parameter(key = "field", label = "Field Name", description = "The field to create or modify.") String fieldName,
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
        FieldValue result = StringFieldValue.of("");
        try {
            // execute the path expression
            Object rawValue = readContext.read(jsonPath);

            // convert the result to a string
            if (rawValue instanceof Collection) {
                List<String> strings = ((Collection<Object>) rawValue).stream()
                        .map(String::valueOf)
                        .collect(Collectors.toList());
                String value = String.join(",", strings);
                result = StringFieldValue.of(value);
            } else if (rawValue instanceof Map){
                String value = objectMapper.writeValueAsString(rawValue);
                result = StringFieldValue.of(value);
            } else {
                String value = String.valueOf(rawValue);
                result = StringFieldValue.of(value);
            }

        } catch (PathNotFoundException e) {
            log.debug("No results for path expression. Nothing to do. jsonPath={}", jsonPath.getPath());
        } catch (JsonProcessingException e) {
            log.debug("Error creating JSON from the Map. jsonPath={}", jsonPath.getPath());
        }
        return result;
    }
}
