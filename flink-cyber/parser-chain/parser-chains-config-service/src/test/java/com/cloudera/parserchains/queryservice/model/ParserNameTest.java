package com.cloudera.parserchains.queryservice.model;

import com.cloudera.parserchains.core.model.define.ParserName;
import com.cloudera.parserchains.core.utils.JSONUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.adrianwalker.multilinestring.Multiline;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.IsEqualCompressingWhiteSpace.equalToCompressingWhiteSpace;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ParserNameTest {

    @Test
    void name() {
        final String expected = "Syslog";
        ParserName name = ParserName.of(expected);
        assertEquals(expected, name.getName());
    }

    /**
     * "Syslog"
     */
    @Multiline
    private String expectedJSON;

    @Test
    void toJSON() throws JsonProcessingException {
        ParserName name = ParserName.of("Syslog");
        String actual = JSONUtils.INSTANCE.toJSON(name, false);
        assertThat(actual, equalToCompressingWhiteSpace(expectedJSON));
    }
}
