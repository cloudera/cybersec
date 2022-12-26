/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

package com.cloudera.parserchains.queryservice.model;

import com.cloudera.parserchains.core.model.define.ParserID;
import com.cloudera.parserchains.core.utils.JSONUtils;
import com.cloudera.parserchains.parsers.SyslogParser;
import com.cyber.jackson.core.JsonProcessingException;
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
