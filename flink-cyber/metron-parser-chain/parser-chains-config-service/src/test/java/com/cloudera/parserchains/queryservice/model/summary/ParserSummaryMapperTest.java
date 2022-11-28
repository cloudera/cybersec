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
