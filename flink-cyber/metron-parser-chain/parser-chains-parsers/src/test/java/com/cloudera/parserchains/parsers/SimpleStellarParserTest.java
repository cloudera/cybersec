package com.cloudera.parserchains.parsers;

import com.cloudera.parserchains.core.FieldName;
import com.cloudera.parserchains.core.FieldValue;
import com.cloudera.parserchains.core.Message;
import com.cloudera.parserchains.core.StringFieldValue;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

import static com.cloudera.parserchains.core.Constants.DEFAULT_INPUT_FIELD;
import static com.cloudera.parserchains.parsers.StellarParserTest.getFileFromResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class SimpleStellarParserTest {

    private static final String SIMPLE_STELLAR_PATH = "/stellar/simple.stellar";
    private static final String COMPLEX_STELLAR_PATH = "/stellar/complex.stellar";

    @Test
    public void testParserWithDefaultInputAndStellarFile() throws IOException {
        String configPath = getFileFromResource(SIMPLE_STELLAR_PATH).getAbsolutePath();

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

    @Test
    public void testParserWithDefaultInputAndStellarFile2() throws IOException {
        String configPath = getFileFromResource(COMPLEX_STELLAR_PATH).getAbsolutePath();

        final SimpleStellarParser simpleStellarParser = new SimpleStellarParser().stellarPath(configPath);
        final LocalDateTime now = LocalDateTime.now(Clock.systemUTC()).minusHours(1);
        final String ts = String.valueOf(now.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli());
        final String tsSolr = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").format(now);
        final String url = "https://www.subdomain.domain.com";
        final String fullHostname = "www.subdomain.domain.com";
        final String domain = "domain.com";
        final HashMap<FieldName, FieldValue> expectedResult = new HashMap<>();
        expectedResult.put(FieldName.of("url"), StringFieldValue.of(url));
        expectedResult.put(FieldName.of("timestamp"), StringFieldValue.of(ts));
        expectedResult.put(FieldName.of("timestamp_solr"), StringFieldValue.of(tsSolr));
        expectedResult.put(FieldName.of("full_hostname"), StringFieldValue.of(fullHostname));
        expectedResult.put(FieldName.of("domain"), StringFieldValue.of(domain));

        final Message output = simpleStellarParser.parse(Message.builder()
                .addField("url", url)
                .addField("timestamp", ts)
                .build());
        assertThat(output.getFields()).containsExactlyInAnyOrderEntriesOf(expectedResult);
        assertThat(output.getError()).isEmpty();
    }

}