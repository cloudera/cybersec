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

import com.cloudera.parserchains.core.FieldName;
import com.cloudera.parserchains.core.FieldValue;
import com.cloudera.parserchains.core.Message;
import com.cloudera.parserchains.core.StringFieldValue;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class TimestampFormatParserTest {

    @Test
    public void testSingleField() {
        Message input = Message.builder()
                .addField(FieldName.of("time"), StringFieldValue.of("2020-08-21T18:51:12.345Z"))
                .build();
        Message output = new TimestampFormatParser()
                .withOutputField("time", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "UTC")
                .parse(input);

        assertThat("Time is correct", output.getField(FieldName.of("time")).get(), equalTo(StringFieldValue.of("1598035872345")));
    }

    @Test
    public void testMultipleFields() {
        Message input = Message.builder()
                .addField(FieldName.of("time"), StringFieldValue.of("2020-08-21T18:51:12.345Z"))
                .addField(FieldName.of("time2"), StringFieldValue.of("2020-07-21T12:34:56.789Z"))
                .build();
        Message output = new TimestampFormatParser()
                .withOutputField("time", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "UTC")
                .withOutputField("time2", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "UTC")
                .parse(input);

        assertThat("Time is correct", output.getField(FieldName.of("time")).get(), equalTo(StringFieldValue.of("1598035872345")));
        assertThat("Time is correct", output.getField(FieldName.of("time2")).get(), equalTo(StringFieldValue.of("1595334896789")));
    }

    @Test
    public void testFallBackFormat() {

        Message input = Message.builder()
                .addField(FieldName.of("time"), StringFieldValue.of("2020-08-21T18:51:12Z"))
                .build();
        Message output = new TimestampFormatParser()
                .withOutputField("time", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z',yyyy-MM-dd'T'HH:mm:ss'Z'", "UTC")
                .parse(input);

        assertThat("Time is correct", output.getField(FieldName.of("time")).get(), equalTo(StringFieldValue.of("1598035872000")));
    }

    @Test
    public void testWildCases() {
        run("2020-08-24T04:40:11.752Z", "1598244011752");
        run("2020-08-24T04:40:11.7Z", "1598244011700");
    }


    private Message run(String in, String expected) {
        Message input = Message.builder()
                .addField(FieldName.of("time"), StringFieldValue.of(in))
                .build();

        Message output = new TimestampFormatParser()
                .withOutputField("time", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z',yyyy-MM-dd'T'HH:mm:ss.SS'Z',yyyy-MM-dd'T'HH:mm:ss.S'Z',yyyy-MM-dd'T'HH:mm:ss'Z'", "UTC")
                .parse(input);

        assertThat("Time is correct", output.getField(FieldName.of("time")).get(), equalTo(StringFieldValue.of(expected)));
        return output;
    }


    @Test
    @Disabled
    public void testWithTimezoneChange() {
        fail("unimplemented");
    }

    @Test
    @Disabled
    public void testDifferentFormats() {
        fail("unimplemented");
    }
}
