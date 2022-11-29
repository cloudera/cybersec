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

package com.cloudera.parserchains.parsers;

import com.cloudera.parserchains.core.*;
import com.github.palindromicity.syslog.SyslogSpecification;
import org.junit.jupiter.api.Test;

import static com.github.palindromicity.syslog.dsl.SyslogFieldKeys.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SyslogParserTest {

    private static final String SYSLOG_5424 = "<14>1 2014-06-20T09:14:07+00:00 loggregator"
            + " d0602076-b14a-4c55-852a-981e7afeed38 DEA MSG-01"
            + " [exampleSDID@32473 iut=\"3\" eventSource=\"Application\" eventID=\"1011\"]"
            + "[exampleSDID@32480 iut=\"4\" eventSource=\"Other Application\" eventID=\"2022\"] "
            + "Removing instance";

    @Test
    void parse5424() {
        Message input = Message.builder()
                .addField(Constants.DEFAULT_INPUT_FIELD, SYSLOG_5424)
                .build();
        Message output = new SyslogParser()
                .parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField(FieldName.of(HEADER_PRI.getField()), StringFieldValue.of("14"))
                .addField(FieldName.of(HEADER_VERSION.getField()), StringFieldValue.of("1"))
                .addField(FieldName.of(HEADER_APPNAME.getField()), StringFieldValue.of("d0602076-b14a-4c55-852a-981e7afeed38"))
                .addField(FieldName.of(HEADER_PROCID.getField()), StringFieldValue.of("DEA"))
                .addField(FieldName.of(HEADER_TIMESTAMP.getField()), StringFieldValue.of("2014-06-20T09:14:07+00:00"))
                .addField(FieldName.of(HEADER_PRI_FACILITY.getField()), StringFieldValue.of("1"))
                .addField(FieldName.of(HEADER_HOSTNAME.getField()), StringFieldValue.of("loggregator"))
                .addField(FieldName.of(HEADER_PRI_SEVERITY.getField()), StringFieldValue.of("6"))
                .addField(FieldName.of(HEADER_MSGID.getField()), StringFieldValue.of("MSG-01"))
                .addField(FieldName.of(STRUCTURED_BASE.getField() + ".exampleSDID@32473.iut"), StringFieldValue.of("3"))
                .addField(FieldName.of(STRUCTURED_BASE.getField() + ".exampleSDID@32473.eventID"), StringFieldValue.of("1011"))
                .addField(FieldName.of(STRUCTURED_BASE.getField() + ".exampleSDID@32473.eventSource"), StringFieldValue.of("Application"))
                .addField(FieldName.of(STRUCTURED_BASE.getField() + ".exampleSDID@32480.iut"), StringFieldValue.of("4"))
                .addField(FieldName.of(STRUCTURED_BASE.getField() + ".exampleSDID@32480.eventID"), StringFieldValue.of("2022"))
                .addField(FieldName.of(STRUCTURED_BASE.getField() + ".exampleSDID@32480.eventSource"), StringFieldValue.of("Other Application"))
                .addField(FieldName.of("syslog.message"), StringFieldValue.of("Removing instance"))
                .build();
        assertEquals(expected, output);
    }

    private static final String SYSLOG_3164 = "<181>2018-09-14T00:54:09+00:00 lzpqrst-admin.in.mycompany.com.lg " +
            "CISE_RADIUS_Accounting 0018032501 1 0 2018-09-14 10:54:09.095 +10:00";

    @Test
    void parse3164() {
        Message input = Message.builder()
                .addField(Constants.DEFAULT_INPUT_FIELD, SYSLOG_3164)
                .build();
        Message output = new SyslogParser()
                .withSpecification(SyslogSpecification.RFC_3164)
                .parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField(FieldName.of(HEADER_PRI.getField()), StringFieldValue.of("181"))
                .addField(FieldName.of(HEADER_PRI_SEVERITY.getField()), StringFieldValue.of("5"))
                .addField(FieldName.of(HEADER_TIMESTAMP.getField()), StringFieldValue.of("2018-09-14T00:54:09+00:00"))
                .addField(FieldName.of(HEADER_PRI_FACILITY.getField()), StringFieldValue.of("22"))
                .addField(FieldName.of(HEADER_HOSTNAME.getField()), StringFieldValue.of("lzpqrst-admin.in.mycompany.com.lg"))
                .addField(FieldName.of(MESSAGE.getField()), StringFieldValue.of("CISE_RADIUS_Accounting 0018032501 1 0 2018-09-14 10:54:09.095 +10:00"))
                .build();
        assertEquals(expected, output);
    }

    @Test
    void parseError() {
        Message input = Message.builder()
                .addField(Constants.DEFAULT_INPUT_FIELD, " SasaS")
                .build();
        Message output = new SyslogParser()
                .parse(input);
        assertTrue(output.getError().isPresent(),
            "Expected a parsing error to have occurred.");
        assertEquals(input.getFields(), output.getFields(),
            "Expected the same input fields to be available on the output message.");
    }

    @Test
    void inputFieldMissing() {
        Message input = Message.builder()
                 .build();
        Message output = new SyslogParser()
                .parse(input);
        assertTrue(output.getError().isPresent(),
                "Expected a parsing error to have occurred.");
        assertEquals(input.getFields(), output.getFields(),
                "Expected the same input fields to be available on the output message.");
    }

    @Test
    void emptyInput() {
        Message input = Message.builder()
                .addField(Constants.DEFAULT_INPUT_FIELD, "")
                .build();
        Message output = new SyslogParser()
                .parse(input);
        assertTrue(output.getError().isPresent(),
                "Expected a parsing error to have occurred.");
        assertEquals(input.getFields(), output.getFields(),
                "Expected the same input fields to be available on the output message.");
    }
}
