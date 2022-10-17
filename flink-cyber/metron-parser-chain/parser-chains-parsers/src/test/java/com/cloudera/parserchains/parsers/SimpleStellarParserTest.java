package com.cloudera.parserchains.parsers;

import com.cloudera.parserchains.core.FieldName;
import com.cloudera.parserchains.core.Message;
import com.cloudera.parserchains.core.StringFieldValue;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.cloudera.parserchains.core.Constants.DEFAULT_INPUT_FIELD;
import static com.cloudera.parserchains.parsers.StellarParserTest.getFileFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class SimpleStellarParserTest {

    private static final String STELLAR_PATH = "/stellar/simple.stellar";

    @Test
    public void testParserWithDefaultInputAndStellarFile() throws IOException {
        String configPath = getFileFromResource(STELLAR_PATH).getAbsolutePath();

        final SimpleStellarParser simpleStellarParser = new SimpleStellarParser().stellarPath(configPath);
        final Message output = simpleStellarParser.parse(Message.builder()
                .addField(DEFAULT_INPUT_FIELD, "test")
                .addField("column2", "lowerCaseText")
                .build());
        assertThat(output.getFields()).contains(
                entry(FieldName.of("upper_col2"), StringFieldValue.of("LOWERCASETEXT")));
        String hash = output.getFields().get(FieldName.of("unique_hash")).get();
        assertThat(hash).matches("^[0-9a-fA-F]{64}$");
        assertThat(hash).doesNotContainPattern("^0{64}$");
    }

}