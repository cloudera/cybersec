package com.cloudera.parserchains.parsers;

import com.cloudera.parserchains.core.*;
import com.cloudera.parserchains.core.catalog.Configurable;
import com.cloudera.parserchains.core.catalog.MessageParser;
import com.cloudera.parserchains.core.catalog.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;

import static com.cloudera.parserchains.core.Constants.DEFAULT_INPUT_FIELD;
import static java.lang.String.format;

@MessageParser(
        name="XPath",
        description="Parse XML using XPath expressions."
)
@Slf4j
public class XPathParser implements Parser {
    private static final String DEFAULT_NAMESPACE_AWARE = "false";
    private LinkedHashMap<FieldName, XPathExpression> compiledExpressions;
    private LinkedHashMap<FieldName, String> expressions;
    private FieldName inputField;
    private XPath xpath;
    private boolean namespaceAware;

    public XPathParser() {
        this.compiledExpressions = new LinkedHashMap<>();
        this.expressions = new LinkedHashMap<>();
        this.inputField = FieldName.of(DEFAULT_INPUT_FIELD);
        this.xpath = XPathFactory.newInstance().newXPath();
        this.namespaceAware = Boolean.valueOf(DEFAULT_NAMESPACE_AWARE);
    }

    @Configurable(
            key="input",
            label="Input Field",
            description="The input field to parse.",
            defaultValue=DEFAULT_INPUT_FIELD)
    public XPathParser inputField(String inputField) {
        if(StringUtils.isNotBlank(inputField)) {
            this.inputField = FieldName.of(inputField);
        }
        return this;
    }

    @Configurable(key="xpath")
    public XPathParser expression(
            @Parameter(key="field", label="Field Name", description="The field to create or modify.") String fieldName,
            @Parameter(key="expr", label="XPath", description="The XPath expression.") String expression) {
        if(StringUtils.isNoneBlank(fieldName, expression)) {
            FieldName field = FieldName.of(fieldName);
            this.compiledExpressions.put(field, compile(expression));
            // save the text of the expression so that it can be reported when an error occurs
            this.expressions.put(field, expression);
        }
        return this;
    }

    private XPathExpression compile(String expression) {
        try {
            return xpath.compile(expression);

        } catch (XPathExpressionException e) {
            log.debug("Failed to compile Xpath expression: {}", expression);
            throw new IllegalArgumentException(e);
        }
    }

    @Configurable(
            key="nsAware",
            label="Namespace Aware",
            description="Should the parser support XML namespaces.",
            defaultValue=DEFAULT_NAMESPACE_AWARE)
    public XPathParser namespaceAware(String namespaceAware) {
        if(StringUtils.isNotBlank(namespaceAware)) {
            this.namespaceAware = Boolean.valueOf(namespaceAware);
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
        Document document = buildDocument(valueToParse, output);
        if(document != null) {
            compiledExpressions.forEach(((field, expr) -> execute(document, field, expr, output)));
        }
    }

    /**
     * Builds the XML document.
     * @param valueToParse The raw XML to parse.
     * @param output The output message.
     * @return
     */
    private Document buildDocument(String valueToParse, Message.Builder output) {
        Document document = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(namespaceAware);
            document = factory.newDocumentBuilder()
                    .parse(IOUtils.toInputStream(valueToParse, Charset.defaultCharset()));

        } catch(ParserConfigurationException | SAXException | IOException e) {
            log.debug("Unable to parse XML document.", e);
            output.withError("Unable to parse XML document.", e);
        }
        return document;
    }

    /**
     * Execute the XPath expression and add the value to the message.
     * @param document The XML document.
     * @param fieldName The name of the field to create or modify.
     * @param expression The XPath expression.
     * @param output The output message.
     */
    private void execute(Document document, FieldName fieldName, XPathExpression expression, Message.Builder output) {
        try {
            String value = (String) expression.evaluate(document, XPathConstants.STRING);
            output.addField(fieldName, StringFieldValue.of(value));

        } catch (XPathExpressionException e) {
            String msg = String.format("Unable to execute XPath expression; %s", expressions.get(fieldName));
            output.withError(msg, e);
            log.debug(msg, e);
        }
    }
}
