package com.cloudera.parserchains.parsers;

import com.cloudera.parserchains.core.*;
import com.cloudera.parserchains.core.catalog.Configurable;
import com.cloudera.parserchains.core.catalog.MessageParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

import static java.lang.String.format;

@MessageParser(
        name="Delimited Key Values",
        description="Parses delimited key-value pairs."
)
public class DelimitedKeyValueParser implements Parser {
    private static final Logger logger = LogManager.getLogger(DelimitedKeyValueParser.class);

    /**
     * The default key-value delimiter are double pipes; ||.
     * <p>Each pipe must be escaped.
     */
    private static final String DEFAULT_DELIMITER = "\\|\\|";
    private static final String DEFAULT_SEPARATOR = "=";
    private FieldName inputField;
    private Regex keyValueDelimiter;
    private Regex keyValueSeparator;
    private Optional<Regex> validKeyRegex;
    private Optional<Regex> validValueRegex;

    public DelimitedKeyValueParser() {
        this.inputField = FieldName.of(Constants.DEFAULT_INPUT_FIELD);
        this.keyValueDelimiter = Regex.of(DEFAULT_DELIMITER);
        this.keyValueSeparator = Regex.of(DEFAULT_SEPARATOR);
        this.validKeyRegex = Optional.empty();
        this.validValueRegex = Optional.empty();
    }

    @Configurable(
            key="input",
            label="Input Field",
            description="The input field to parse.",
            defaultValue=Constants.DEFAULT_INPUT_FIELD)
    public DelimitedKeyValueParser inputField(String inputField) {
        if(StringUtils.isNotBlank(inputField)) {
            this.inputField = FieldName.of(inputField);
        }
        return this;
    }

    public FieldName getInputField() {
        return inputField;
    }

    @Configurable(
            key="delimiter",
            label="Key Value Delimiter",
            description="A regex that separates different key-value pairs.",
            defaultValue=DEFAULT_DELIMITER
    )
    public DelimitedKeyValueParser keyValueDelimiter(String keyValueDelimiter) {
        if(StringUtils.isNotBlank(keyValueDelimiter)) {
            this.keyValueDelimiter = Regex.of(keyValueDelimiter);
        }
        return this;
    }

    public Regex getKeyValueDelimiter() {
        return keyValueDelimiter;
    }

    @Configurable(
            key="separator",
            label="Key Value Separator",
            description="A regex that separates a key and value within a key-value pair.",
            defaultValue=DEFAULT_SEPARATOR
    )
    public DelimitedKeyValueParser keyValueSeparator(String keyValueSeparator) {
        if(StringUtils.isNotBlank(keyValueSeparator)) {
            this.keyValueSeparator = Regex.of(keyValueSeparator);
        }
        return this;
    }

    public Regex getKeyValueSeparator() {
        return keyValueSeparator;
    }

    @Configurable(
            key="validKey",
            label="Valid Key Regex",
            description="Any key not matching this regex will be ignored."
    )
    public DelimitedKeyValueParser validKeyRegex(String validKeyRegex) {
        if(StringUtils.isNotBlank(validKeyRegex)) {
            this.validKeyRegex = Optional.ofNullable(Regex.of(validKeyRegex));
        } else {
            // a blank string should 'turn off' regex matching
            this.validKeyRegex = Optional.empty();
        }
        return this;
    }

    public Optional<Regex> getValidKeyRegex() {
        return validKeyRegex;
    }

    @Configurable(
            key="validValue",
            label="Valid Value Regex",
            description="Any value not matching this regex will be ignored."
    )
    public DelimitedKeyValueParser validValueRegex(String validValueRegex) {
        if(StringUtils.isNotBlank(validValueRegex)) {
            this.validValueRegex = Optional.ofNullable(Regex.of(validValueRegex));
        } else {
            // a blank string should 'turn off' regex matching
            this.validValueRegex = Optional.empty();
        }
        return this;
    }

    public Optional<Regex> getValidValueRegex() {
        return validValueRegex;
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
        String[] keyValuePairs = valueToParse.split(keyValueDelimiter.toString());
        logger.debug("Found {} key-value pairs.", keyValuePairs.length);

        for(String keyValuePair: keyValuePairs) {

            String [] keyValue = keyValuePair.split(keyValueSeparator.toString(), 2);
            if(keyValue.length == 2) {
                final String key = keyValue[0];
                final String value = keyValue[1];

                boolean validKey = validKeyRegex.map(regex -> regex.matches(key)).orElse(true);
                boolean validValue = validValueRegex.map(regex -> regex.matches(value)).orElse(true);
                if(validKey && validValue) {
                    try {
                        FieldName fieldName = FieldName.of(key);
                        FieldValue fieldValue = FieldValue.of(value);
                        output.addField(fieldName, fieldValue);

                    } catch(IllegalArgumentException e) {
                        logger.debug("Ignoring an invalid key-value pair; '{}'", keyValuePair);
                    }
                } else {
                    logger.debug("Ignoring an invalid key-value pair; '{}'", keyValuePair);
                }
            } else {
                logger.debug("Ignoring a missing key-value pair; '{}'", keyValuePair);
            }
        }
    }
}
