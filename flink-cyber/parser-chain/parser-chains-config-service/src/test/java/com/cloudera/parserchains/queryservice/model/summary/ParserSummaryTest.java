package com.cloudera.parserchains.queryservice.model.summary;

import com.cloudera.parserchains.core.model.define.ParserID;
import com.cloudera.parserchains.core.utils.JSONUtils;
import com.cloudera.parserchains.parsers.SyslogParser;
import org.adrianwalker.multilinestring.Multiline;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.IsEqualCompressingWhiteSpace.equalToCompressingWhiteSpace;

public class ParserSummaryTest {

    /**
     * {
     *  "id" : "com.cloudera.parserchains.parsers.SyslogParser",
     *  "name" : "Syslog"
     * }
     */
    @Multiline
    private String expectedJSON;

    @Test
    void toJSON() throws Exception {
        ParserSummary parserSummary = new ParserSummary()
                .setName("Syslog")
                .setDescription("Parses Syslog according to RFC 3164 and 5424.")
                .setId((ParserID.of(SyslogParser.class)));

        String actual = JSONUtils.INSTANCE.toJSON(parserSummary, true);
        assertThat(actual, equalToCompressingWhiteSpace(expectedJSON));
    }
}
