package com.cloudera.parserchains.queryservice.model.summary;

import com.cloudera.parserchains.core.catalog.ParserInfo;
import com.cloudera.parserchains.core.model.define.ParserID;
import com.cloudera.parserchains.parsers.SyslogParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ParserSummaryMapperTest {

    @Test
    void reform() {
        ParserInfo parserInfo = ParserInfo
                .builder()
                .name("Syslog")
                .description("Parses Syslog according to RFC 3164 and 5424.")
                .parserClass(SyslogParser.class)
                .build();
        ParserSummary expected = new ParserSummary()
                .setName("Syslog")
                .setDescription("Parses Syslog according to RFC 3164 and 5424.")
                .setId(ParserID.of(SyslogParser.class));
        assertEquals(expected, new ParserSummaryMapper().reform(parserInfo));
    }

    @Test
    void transform() {
        ParserSummary parserSummary = new ParserSummary()
                .setName("Syslog")
                .setDescription("Parses Syslog according to RFC 3164 and 5424.")
                .setId(ParserID.of(SyslogParser.class));
        ParserInfo expected = ParserInfo
                .builder()
                .name("Syslog")
                .description("Parses Syslog according to RFC 3164 and 5424.")
                .parserClass(SyslogParser.class)
                .build();
        assertEquals(expected, new ParserSummaryMapper().transform(parserSummary));
    }

    @Test
    void classNotAParser() {
        ParserSummary parserSummary = new ParserSummary()
                .setName("Syslog")
                .setDescription("Parses Syslog according to RFC 3164 and 5424.")
                .setId((ParserID.of(ParserSummaryMapperTest.class)));
        assertThrows(IllegalArgumentException.class, () -> new ParserSummaryMapper().transform(parserSummary));
    }
}
