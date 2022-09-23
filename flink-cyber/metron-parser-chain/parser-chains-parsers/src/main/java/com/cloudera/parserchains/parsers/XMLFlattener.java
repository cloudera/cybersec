package com.cloudera.parserchains.parsers;

import com.cloudera.parserchains.core.Constants;
import com.cloudera.parserchains.core.FieldName;
import com.cloudera.parserchains.core.FieldValue;
import com.cloudera.parserchains.core.Message;
import com.cloudera.parserchains.core.Parser;
import com.cloudera.parserchains.core.StringFieldValue;
import com.cloudera.parserchains.core.catalog.Configurable;
import com.cloudera.parserchains.core.catalog.MessageParser;
import com.github.wnameless.json.flattener.JsonFlattener;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.json.XMLParserConfiguration;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@MessageParser(
        name = "XML Flattener",
        description = "Flattens XML data."
)
@Slf4j
public class XMLFlattener implements Parser {
    private static final String DEFAULT_SEPARATOR = ".";
    private static final String EMPTY_ELEMENT_REGEX = "\\{ *\\}";
    private static final XMLParserConfiguration XML_CONFIG = XMLParserConfiguration.KEEP_STRINGS.withcDataTagName("");
    private FieldName inputField;
    private char separator = '|';

    public XMLFlattener() {
        inputField(Constants.DEFAULT_INPUT_FIELD);
        separator(DEFAULT_SEPARATOR);
    }

    @Configurable(key = "inputField",
            label = "Input Field",
            description = "The name of the input field to parse.",
            defaultValue = Constants.DEFAULT_INPUT_FIELD)
    public XMLFlattener inputField(String fieldName) {
        if (StringUtils.isNotEmpty(fieldName)) {
            this.inputField = FieldName.of(fieldName);
        }
        return this;
    }

    @Configurable(key = "separator",
            label = "Separator",
            description = "The character used to separate each nested XML element.",
            defaultValue = DEFAULT_SEPARATOR
    )
    public XMLFlattener separator(String separator) {
        if (StringUtils.isNotEmpty(separator)) {
            this.separator = separator.charAt(0);
        }
        return this;
    }

    @Override
    public Message parse(Message input) {
        Message.Builder output = Message.builder().withFields(input);
        final Optional<FieldValue> field = input.getField(inputField);
        if (!field.isPresent()) {
            output.withError(format("Message missing expected input field '%s'", inputField.toString()));
        } else {
            doParse(field.get().toString(), output);
        }
        return output.build();
    }

    private void doParse(String valueToParse, Message.Builder output) {
        try {
            // parse the XML
            final JSONObject result = XML.toJSONObject(valueToParse, XML_CONFIG);

            // flatten the JSON
            final String json = result.toString();
            Map<String, Object> values = new JsonFlattener(json)
                    .withSeparator(separator)
                    .flattenAsMap();

            // add each value to the message
            values.entrySet()
                    .stream()
                    .filter(e -> e.getValue() != null && isNotBlank(e.getKey()))
                    .forEach(e -> output.addField(fieldName(e.getKey()), fieldValue(e.getValue())));

        } catch (JSONException e) {
            output.withError("Unable to convert XML to JSON.", e);
        }
    }

    private FieldName fieldName(String key) {
        // remove any trailing separators
        String fieldName = key.replaceFirst("\\" + separator + "$", "");
        return FieldName.of(fieldName);
    }

    private FieldValue fieldValue(Object value) {
        // ignore an empty element like {}
        String fieldValue = Objects.toString(value).replaceFirst(EMPTY_ELEMENT_REGEX, "");
        return StringFieldValue.of(fieldValue);
    }

}
