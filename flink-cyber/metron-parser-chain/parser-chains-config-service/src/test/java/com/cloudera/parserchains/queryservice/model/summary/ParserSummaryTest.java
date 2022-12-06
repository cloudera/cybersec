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
