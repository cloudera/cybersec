package com.cloudera.cyber.stellar;


import com.cloudera.parserchains.core.StringFieldValue;
import org.apache.metron.parsers.interfaces.MessageParserResult;
import org.apache.metron.stellar.common.JSONMapObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class MetronCompatibilityParserTest {
    private static final String CONFIG_FILE = "/stellar/test_metron_parser_config.json";
    private static final String BAD_SYNTAX_CONFIG_FILE = "/stellar/test_metron_bad_json_parser_config.json";
    private static final String PARSER_CLASS_NOT_DEFINED = "/stellar/test_metron_parser_class_not_defined_config.json";

    @Test
    public void testParser() throws IOException {
        MetronCompatibilityParser parser = MetronCompatibilityParser.of("test", configStream(CONFIG_FILE));
        String timestamp = "1617059998456";
        String column1 = "value_1";
        String column2 = "value_2";
        String originalString = String.format("%s %s %s", timestamp, column1, column2);
        Optional<MessageParserResult<JSONMapObject>> optionalResult = parser.parse(StringFieldValue.of(originalString).toMessageToParse());
        assertTrue(optionalResult.isPresent());
        assertEquals(optionalResult.get().getMessages().size(), 1);
        JSONMapObject message = optionalResult.get().getMessages().get(0);
        assertEquals(message.get("timestamp"), Long.valueOf(timestamp));
        assertEquals(message.get("original_string"), originalString);
        assertTrue((boolean) message.get("initialized"));
        assertTrue((boolean) message.get("configured"));
        assertEquals(message.get("column1"), column1);
        assertEquals(message.get("column2"), column2.toUpperCase());
    }

    @Test
    public void testBadJsonConfig() {
        assertThrows(com.fasterxml.jackson.core.JsonParseException.class,
                () -> MetronCompatibilityParser.of("badsyntax", configStream(BAD_SYNTAX_CONFIG_FILE)));
    }

    @Test
    public void testClassNotFoundConfig() {
        String expectedMessage = "Unable to instantiate connector: class not found";
        assertThrows(IllegalStateException.class,
                () -> MetronCompatibilityParser.of("parsernotdefined", configStream( PARSER_CLASS_NOT_DEFINED)),
                expectedMessage);
    }

    private InputStream configStream(String configUri) {
        return MetronCompatibilityParser.class.getResourceAsStream(configUri);
    }
}
