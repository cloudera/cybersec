package com.cloudera.parserchains.parsers;

import com.cloudera.parserchains.core.*;
import com.cloudera.parserchains.core.catalog.Configurable;
import com.cloudera.parserchains.core.catalog.MessageParser;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.JsonNodeDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import com.github.wnameless.json.flattener.JsonFlattener;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@MessageParser(
        name="XML Flattener",
        description="Flattens XML data."
)
public class XMLFlattener implements Parser {
    private static final Logger logger = LogManager.getLogger(XMLFlattener.class);
    private static final String DEFAULT_SEPARATOR = ".";
    private static final String EMPTY_ELEMENT_REGEX = "\\{ *\\}";
    private FieldName inputField;
    private char separator = '|';

    public XMLFlattener() {
        inputField(Constants.DEFAULT_INPUT_FIELD);
        separator(DEFAULT_SEPARATOR);
    }

    @Configurable(key="inputField",
            label="Input Field",
            description="The name of the input field to parse.",
            defaultValue=Constants.DEFAULT_INPUT_FIELD)
    public XMLFlattener inputField(String fieldName) {
        if(StringUtils.isNotEmpty(fieldName)) {
            this.inputField = FieldName.of(fieldName);
        }
        return this;
    }

    @Configurable(key="separator",
            label="Separator",
            description="The character used to separate each nested XML element.",
            defaultValue=DEFAULT_SEPARATOR
    )
    public XMLFlattener separator(String separator) {
        if(StringUtils.isNotEmpty(separator)) {
            this.separator = separator.charAt(0);
        }
        return this;
    }

    @Override
    public Message parse(Message input) {
        Message.Builder output = Message.builder().withFields(input);
        if(!input.getField(inputField).isPresent()) {
            output.withError(format("Message missing expected input field '%s'", inputField.toString()));
        } else {
            input.getField(inputField).ifPresent(val -> doParse(val.toString(), output));
        }
        return output.build();
    }

    private void doParse(String valueToParse, Message.Builder output) {
        try {
            // parse the XML
            Module customModule = new SimpleModule()
                    .addDeserializer(JsonNode.class, new CustomJsonNodeDeserializer());
            JsonNode node = new XmlMapper()
                    .registerModule(customModule)
                    .readTree(valueToParse);

            // flatten the JSON
            String json = new ObjectMapper().writeValueAsString(node);
            Map<String, Object> values = new JsonFlattener(json)
                    .withSeparator(separator)
                    .flattenAsMap();

            // add each value to the message
            values.entrySet()
                    .stream()
                    .filter(e -> e.getValue() != null && isNotBlank(e.getKey()))
                    .forEach(e -> output.addField(fieldName(e.getKey()), fieldValue(e.getValue())));

        } catch(JsonProcessingException e) {
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
        return FieldValue.of(fieldValue);
    }

    public class CustomJsonNodeDeserializer extends JsonNodeDeserializer {

        @Override
        public JsonNode deserialize(JsonParser p, DeserializationContext context) throws IOException {
            // ensures that we do not lose the name of the root XML element
            String rootName = ((FromXmlParser)p).getStaxReader().getLocalName();
            JsonNode rootNode = super.deserialize(p, context);
            return context.getNodeFactory().objectNode().set(rootName, rootNode);
        }

        @Override
        protected void _handleDuplicateField(JsonParser p,
                                             DeserializationContext context,
                                             JsonNodeFactory nodeFactory,
                                             String fieldName,
                                             ObjectNode objectNode,
                                             JsonNode oldValue,
                                             JsonNode newValue) {
            // adds duplicate fields to an array
            ArrayNode node;
            if(oldValue instanceof ArrayNode){
                node = (ArrayNode) oldValue;
            } else {
                node = nodeFactory.arrayNode();
                node.add(oldValue);
            }
            node.add(newValue);
            objectNode.set(fieldName, node);
        }
    }
}
