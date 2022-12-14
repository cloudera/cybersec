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
import com.cloudera.parserchains.core.Message;
import com.cloudera.parserchains.core.StringFieldValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class TimestampParserTest {

    public static class FixedClock extends TimestampParser.Clock {
        private long currentTimeMillis;

        public FixedClock(long currentTimeMillis) {
            this.currentTimeMillis = currentTimeMillis;
        }

        @Override
        public long currentTimeMillis() {
            return currentTimeMillis;
        }
    }

    @Test
    void addTimestamp() {
        long time = 1426349294842L;
        Message input = Message.builder()
                .addField(FieldName.of("field1"), StringFieldValue.of("value1"))
                .build();
        Message output = new TimestampParser()
                .withClock(new FixedClock(time))
                .withOutputField("processing_timestamp")
                .parse(input);
        assertEquals(StringFieldValue.of(Long.toString(time)), output.getField(FieldName.of("processing_timestamp")).get(),
            "Expected a timestamp to have been added to the message.");
        assertEquals(StringFieldValue.of("value1"), output.getField(FieldName.of("field1")).get(),
            "Expected the same input fields to be available on the output message.");
        assertFalse(output.getError().isPresent(),
            "Expected no errors to have occurred.");
    }

    @Test
    void defaultTimestampField() {
        long time = 1426349294842L;
        Message input = Message.builder()
                .addField(FieldName.of("field1"), StringFieldValue.of("value1"))
                .build();
        TimestampParser parser = new TimestampParser();
        Message output = parser
                .withClock(new FixedClock(time))
                .parse(input);
        FieldName defaultFieldName = parser.getOutputField();
        assertEquals(StringFieldValue.of(Long.toString(time)), output.getField(defaultFieldName).get(),
                "Expected a timestamp to have been added using the default field name.");
        assertEquals(StringFieldValue.of("value1"), output.getField(FieldName.of("field1")).get(),
                "Expected the same input fields to be available on the output message.");
        assertFalse(output.getError().isPresent(),
                "Expected no errors to have occurred.");
    }

    @Test
    void configureTimestampField() {
        TimestampParser parser = new TimestampParser();
        parser.withOutputField("processing_time");
        assertEquals(FieldName.of("processing_time"), parser.getOutputField());
    }
}
