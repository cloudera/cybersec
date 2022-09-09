package com.cloudera.parserchains.parsers;

import com.cloudera.parserchains.core.FieldName;
import com.cloudera.parserchains.core.FieldValue;
import com.cloudera.parserchains.core.Message;
import com.cloudera.parserchains.core.StringFieldValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvTextParserTest {
    private CsvTextParser parser;

    @BeforeEach
    void beforeEach() {
        parser = new CsvTextParser();
    }

    @Test
    void parseCSV() {
        FieldValue csvToParse = StringFieldValue.of("value1, value2, value3, value4");
        Message input = Message.builder()
                .addField(FieldName.of("input"), csvToParse)
                .build();

        // parse the message
        Message output = parser
                .withDelimiter(',')
                .withInputField(FieldName.of("input"))
                .withOutputField(FieldName.of("first"), 0)
                .withOutputField(FieldName.of("second"), 1)
                .withOutputField(FieldName.of("third"), 2)
                .parse(input);

        assertEquals(csvToParse, output.getField(FieldName.of("input")).get(),
                "Expected the 'input' field to remain in the output message.");
        assertEquals(StringFieldValue.of("value1"), output.getField(FieldName.of("first")).get(),
                "Expected the 'first' field to have been added to the message.");
        assertEquals(StringFieldValue.of("value2"), output.getField(FieldName.of("second")).get(),
                "Expected the 'second' field to have been added to the message.");
        assertEquals(StringFieldValue.of("value3"), output.getField(FieldName.of("third")).get(),
                "Expected the 'third' field to have been added to the message.");
        assertFalse(output.getError().isPresent(),
                "Expected no parsing errors.");
    }

    @Test
    void parseCSVWithEscapeChars() {
        FieldValue csvToParse = StringFieldValue.of("value1,\"value2, value 3\", value4");
        Message input = Message.builder()
                .addField(FieldName.of("input"), csvToParse)
                .build();

        // parse the message
        Message output = parser
                .withDelimiter(',')
                .withInputField(FieldName.of("input"))
                .withOutputField(FieldName.of("first"), 0)
                .withOutputField(FieldName.of("second"), 1)
                .withOutputField(FieldName.of("third"), 2)
                .parse(input);

        assertEquals(csvToParse, output.getField(FieldName.of("input")).get(),
                "Expected the 'input' field to remain in the output message.");
        assertEquals(StringFieldValue.of("value1"), output.getField(FieldName.of("first")).get(),
                "Expected the 'first' field to have been added to the message.");
        assertEquals(StringFieldValue.of("value2, value 3"), output.getField(FieldName.of("second")).get(),
                "Expected the 'second' field to have been added to the message.");
        assertEquals(StringFieldValue.of("value4"), output.getField(FieldName.of("third")).get(),
                "Expected the 'third' field to have been added to the message.");
        assertFalse(output.getError().isPresent(),
                "Expected no parsing errors.");
    }

    @Test
    void parseCSVWithEscapeCharsAndCustomQuotes() {
        FieldValue csvToParse = StringFieldValue.of("value1,\tvalue2, \"value 3\"\t, value4");
        Message input = Message.builder()
                .addField(FieldName.of("input"), csvToParse)
                .build();

        // parse the message
        Message output = parser
                .withDelimiter(',')
                .withInputField(FieldName.of("input"))
                .withOutputField(FieldName.of("first"), 0)
                .withOutputField(FieldName.of("second"), 1)
                .withOutputField(FieldName.of("third"), 2)
                .withQuoteChar('\t')
                .parse(input);

        assertEquals(csvToParse, output.getField(FieldName.of("input")).get(),
                "Expected the 'input' field to remain in the output message.");
        assertEquals(StringFieldValue.of("value1"), output.getField(FieldName.of("first")).get(),
                "Expected the 'first' field to have been added to the message.");
        assertEquals(StringFieldValue.of("value2, \"value 3\""), output.getField(FieldName.of("second")).get(),
                "Expected the 'second' field to have been added to the message.");
        assertEquals(StringFieldValue.of("value4"), output.getField(FieldName.of("third")).get(),
                "Expected the 'third' field to have been added to the message.");
        assertFalse(output.getError().isPresent(),
                "Expected no parsing errors.");
    }

    @Test
    void parseCSVWithEscapeCharsAndWhitespaces() {
        FieldValue csvToParse = StringFieldValue.of("value1,\" value2, value 3     \", value4");
        Message input = Message.builder()
                .addField(FieldName.of("input"), csvToParse)
                .build();

        // parse the message
        Message output = parser
                .withDelimiter(',')
                .withInputField(FieldName.of("input"))
                .withOutputField(FieldName.of("first"), 0)
                .withOutputField(FieldName.of("second"), 1)
                .withOutputField(FieldName.of("third"), 2)
                .trimWhitespace(true)
                .parse(input);

        assertEquals(csvToParse, output.getField(FieldName.of("input")).get(),
                "Expected the 'input' field to remain in the output message.");
        assertEquals(StringFieldValue.of("value1"), output.getField(FieldName.of("first")).get(),
                "Expected the 'first' field to have been added to the message.");
        assertEquals(StringFieldValue.of("value2, value 3"), output.getField(FieldName.of("second")).get(),
                "Expected the 'second' field to have been added to the message.");
        assertEquals(StringFieldValue.of("value4"), output.getField(FieldName.of("third")).get(),
                "Expected the 'third' field to have been added to the message.");
        assertFalse(output.getError().isPresent(),
                "Expected no parsing errors.");
    }

    @Test
    void parseTSV() {
        FieldValue tsvToParse = StringFieldValue.of("value1\t value2\t value3\t value4");
        Message input = Message.builder()
                .addField(FieldName.of("input"), tsvToParse)
                .build();

        // parse the message
        Message output = parser
                .withDelimiter('\t')
                .withInputField(FieldName.of("input"))
                .withOutputField(FieldName.of("first"), 0)
                .withOutputField(FieldName.of("second"), 1)
                .withOutputField(FieldName.of("third"), 2)
                .parse(input);

        assertEquals(tsvToParse, output.getField(FieldName.of("input")).get(),
                "Expected the 'input' field to remain in the output message.");
        assertEquals(StringFieldValue.of("value1"), output.getField(FieldName.of("first")).get(),
                "Expected the 'first' field to have been added to the message.");
        assertEquals(StringFieldValue.of("value2"), output.getField(FieldName.of("second")).get(),
                "Expected the 'second' field to have been added to the message.");
        assertEquals(StringFieldValue.of("value3"), output.getField(FieldName.of("third")).get(),
                "Expected the 'third' field to have been added to the message.");
        assertFalse(output.getError().isPresent(),
                "Expected no parsing errors.");
    }

    @Test
    void missingInputField() {
        Message input = Message.builder()
                .build();

        // parse the message
        Message output = parser
                .withDelimiter(',')
                .withInputField(FieldName.of("input"))
                .withOutputField(FieldName.of("first"), 0)
                .parse(input);

        assertTrue(output.getError().isPresent(),
                "Expected a parsing error because there is no 'input' field to parse.");
    }

    @Test
    void missingOutputField() {
        FieldValue csvToParse = StringFieldValue.of("value1, value2, value3, value4");
        Message input = Message.builder()
                .addField(FieldName.of("input"), csvToParse)
                .build();

        // there are only 4 fields in the CSV, but trying to use an index of 10
        int index = 10;
        Message output = parser
                .withDelimiter(',')
                .withInputField(FieldName.of("input"))
                .withOutputField(FieldName.of("first"), index)
                .parse(input);

        assertEquals(csvToParse, output.getField(FieldName.of("input")).get(),
                "Expected the 'input' field to remain in the output message.");
        assertTrue(output.getError().isPresent(),
                "Expected a parsing error because column '10' does not exist in the data.");
    }

    @Test
    void emptyInputField() {
        // the input field is empty
        FieldValue csvToParse = StringFieldValue.of("");
        Message input = Message.builder()
                .addField(FieldName.of("input"), csvToParse)
                .build();

        // parse the message
        Message output = parser
                .withDelimiter(',')
                .withInputField(FieldName.of("input"))
                .withOutputField(FieldName.of("first"), 0)
                .withOutputField(FieldName.of("second"), 1)
                .withOutputField(FieldName.of("third"), 2)
                .parse(input);

        assertEquals(csvToParse, output.getField(FieldName.of("input")).get(),
                "Expected the 'input' field to remain in the output message.");
        assertTrue(output.getError().isPresent(),
                "Expected a parsing error because the input field is empty.");
    }

    @Test
    void noWhitespaceTrim() {
        FieldValue csvToParse = StringFieldValue.of(" value1, value2, value3, value4");
        Message input = Message.builder()
                .addField(FieldName.of("input"), csvToParse)
                .build();

        // parse the message
        Message output = parser
                .trimWhitespace(false)
                .withDelimiter(',')
                .withInputField(FieldName.of("input"))
                .withOutputField(FieldName.of("first"), 0)
                .withOutputField(FieldName.of("second"), 1)
                .withOutputField(FieldName.of("third"), 2)
                .parse(input);

        assertEquals(csvToParse, output.getField(FieldName.of("input")).get(),
                "Expected the 'input' field to remain in the output message.");
        assertEquals(StringFieldValue.of(" value1"), output.getField(FieldName.of("first")).get(),
                "Expected the 'first' field to have been added to the message.");
        assertEquals(StringFieldValue.of(" value2"), output.getField(FieldName.of("second")).get(),
                "Expected the 'second' field to have been added to the message.");
        assertEquals(StringFieldValue.of(" value3"), output.getField(FieldName.of("third")).get(),
                "Expected the 'third' field to have been added to the message.");
        assertFalse(output.getError().isPresent(),
                "Expected no parsing errors.");
    }

    @Test
    void outputFieldNotDefined() {
        Message input = Message.builder()
                .build();
        Message output = parser
                .withInputField(FieldName.of("input"))
                .withDelimiter(',')
                .parse(input);
        assertTrue(output.getError().isPresent(),
                "Expected parsing error because no output field(s) have been defined");
    }

    @Test
    void configureInputField() {
        parser.withInputField("cheese");
        assertEquals(FieldName.of("cheese"), parser.getInputField());
    }

    @Test
    void configureOutputFields() {
        // define the output field values
        parser.withOutputField("first", "0");
        parser.withOutputField("second", "1");
        parser.withOutputField("third", "2");

        List<CsvTextParser.OutputField> outputFields = parser.getOutputFields();
        assertEquals(3, outputFields.size());
        assertEquals(FieldName.of("first"), outputFields.get(0).fieldName);
        assertEquals(0, outputFields.get(0).index);
        assertEquals(FieldName.of("second"), outputFields.get(1).fieldName);
        assertEquals(1, outputFields.get(1).index);
        assertEquals(FieldName.of("third"), outputFields.get(2).fieldName);
        assertEquals(2, outputFields.get(2).index);
    }

    @Test
    void configureQuoteChar() {
        parser.withQuoteChar("\\t");
        assertEquals('\t', parser.getQuoteChar());
    }

    @Test
    void configureDelimiter() {
        parser.withDelimiter("|");
        assertEquals('|', parser.getDelimiter());
    }

    @Test
    void configureTrim() {
        parser.trimWhitespace("false");
        assertFalse(parser.isTrimWhitespace());
    }
}