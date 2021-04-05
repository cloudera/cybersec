package com.cloudera.parserchains.parsers;

import com.cloudera.parserchains.core.FieldName;
import com.cloudera.parserchains.core.FieldValue;
import com.cloudera.parserchains.core.Message;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static com.cloudera.parserchains.core.Constants.DEFAULT_INPUT_FIELD;
import static org.assertj.core.api.Assertions.*;

public class StellarParserTest {

    private static final String CONFIG_PATH = "/stellar/test_stellar_parser_config.json";
    private static final String SIMPLE_CONFIG_PATH = "/stellar/test_simple_stellar_parser_config.json";
    private static final String DOESNT_EXIST_CONFIG_FILE = "/stellar/doesnt_exist.json";

    @Test
    public void testParserWithDefaultInputAndStellarConfig() throws IOException {
        String configPath = getFileFromResource(CONFIG_PATH).getAbsolutePath();

        Message output = verifyParser(DEFAULT_INPUT_FIELD, configPath);
        assertThat(output.getFields()).contains(
                entry(FieldName.of("upper_col2"), FieldValue.of("VALUE_2")));
        String hash = output.getFields().get(FieldName.of("unique_hash")).get();
        assertThat(hash).matches("^[0-9a-fA-F]{64}$");
        assertThat(hash).doesNotContainPattern("^0{64}$");
    }

    @Test
    public void testParserWithDefaultInputAndSimpleConfig() throws IOException {
        String configPath = getFileFromResource(SIMPLE_CONFIG_PATH).getAbsolutePath();

        verifyParser(DEFAULT_INPUT_FIELD, configPath);

    }

    @Test
    public void testParserWithNonDefaultInputField() throws IOException {
        String configPath = getFileFromResource(SIMPLE_CONFIG_PATH).getAbsolutePath();

        verifyParser("custom_input", configPath);
    }

    @Test
    public void testParserErrorConfigFileDoesNotExist() {
        assertThatThrownBy(() -> new StellarParser().configurationPath(DOESNT_EXIST_CONFIG_FILE)).
                isInstanceOf(FileNotFoundException.class).hasMessageContaining(String.format("%s (No such file or directory)", DOESNT_EXIST_CONFIG_FILE));
    }

    @Test
    public void testOriginalInputFieldNotFound() throws IOException {
        String configPath = getFileFromResource(SIMPLE_CONFIG_PATH).getAbsolutePath();
        Message output = new StellarParser().configurationPath(configPath).parse(Message.builder().build());
        assertThat(output.getError()).isPresent();
        assertThat(output.getError().get()).hasMessageStartingWith("Message missing expected input field ");
    }

    @Test
    public void testParserReturnsNull() throws IOException {
        String configPath = getFileFromResource(SIMPLE_CONFIG_PATH).getAbsolutePath();
        Message input = Message.builder().addField(DEFAULT_INPUT_FIELD, "null").build();
        Message output = new StellarParser().configurationPath(configPath).parse(input);
        assertThat(output.getError()).isPresent();
        assertThat(output.getError().get()).hasMessageStartingWith("Parser did not return a message result");
    }

    @Test
    public void testParserReturnsEmpty() throws IOException {
        String configPath = getFileFromResource(SIMPLE_CONFIG_PATH).getAbsolutePath();
        Message input = Message.builder().addField(DEFAULT_INPUT_FIELD, "empty").build();
        Message output = new StellarParser().configurationPath(configPath).parse(input);
        assertThat(output.getError()).isPresent();
        assertThat(output.getError().get()).hasMessageStartingWith("Parser returned an empty message result");
    }

    @Test
    public void testParserThrows() throws IOException {
        String configPath = getFileFromResource(SIMPLE_CONFIG_PATH).getAbsolutePath();
        Message input = Message.builder().addField(DEFAULT_INPUT_FIELD, "throw").build();
        Message output = new StellarParser().configurationPath(configPath).parse(input);
        assertThat(output.getError()).isPresent();
        assertThat(output.getError().get()).hasMessageStartingWith("Example exception");
    }

    private Message verifyParser(String inputField, String configPath) throws IOException {
        String timestamp = "1617059998456";
        String originalString = String.format("%s value_1 value_2", timestamp);
        Message input = Message.builder()
                .addField(FieldName.of(inputField), FieldValue.of(originalString))
                .build();
        StellarParser stellarParser = new StellarParser().configurationPath(configPath);
        if (!DEFAULT_INPUT_FIELD.equals(inputField)) {
            stellarParser.inputField(inputField);
        }
        Message output = stellarParser.parse(input);
        assertThat(output.getFields()).contains(
                entry(FieldName.of("timestamp"), FieldValue.of(timestamp)),
                entry(FieldName.of("column1"), FieldValue.of("value_1")),
                entry(FieldName.of("column2"), FieldValue.of("value_2")),
                entry(FieldName.of("initialized"), FieldValue.of("true")),
                entry(FieldName.of("a"), FieldValue.of("a config")),
                entry(FieldName.of("b"), FieldValue.of("b config")),
                entry(FieldName.of("original_string"), FieldValue.of(originalString)));

        return output;
    }

    private static File getFileFromResource(String path) {
        return new File(StellarParserTest.class.getResource(path).getFile());
    }

}
