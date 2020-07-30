package com.cloudera.parserchains.queryservice.model;

import com.cloudera.parserchains.core.model.define.ParserID;
import com.cloudera.parserchains.core.utils.JSONUtils;
import com.cloudera.parserchains.parsers.SyslogParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.adrianwalker.multilinestring.Multiline;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.IsEqualCompressingWhiteSpace.equalToCompressingWhiteSpace;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ParserIDTest {

    @Test
    void fromClassName() {
        ParserID id = ParserID.of(ParserIDTest.class);
        assertEquals("com.cloudera.parserchains.queryservice.model.ParserIDTest", id.getId());
    }

    @Test
    void fromString() {
        ParserID id = ParserID.of(SyslogParser.class);
        assertEquals("com.cloudera.parserchains.parsers.SyslogParser", id.getId());
    }

    /**
     * "com.cloudera.parserchains.parsers.SyslogParser"
     */
    @Multiline
    private String expectedJSON;

    @Test
    void toJSON() throws JsonProcessingException {
        ParserID id = ParserID.of(SyslogParser.class);
        String actual = JSONUtils.INSTANCE.toJSON(id, false);
        assertThat(actual, equalToCompressingWhiteSpace(expectedJSON));
    }
}
