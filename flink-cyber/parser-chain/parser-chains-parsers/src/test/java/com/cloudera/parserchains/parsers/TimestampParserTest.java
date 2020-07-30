package com.cloudera.parserchains.parsers;

import com.cloudera.parserchains.core.FieldName;
import com.cloudera.parserchains.core.FieldValue;
import com.cloudera.parserchains.core.Message;
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
                .addField(FieldName.of("field1"), FieldValue.of("value1"))
                .build();
        Message output = new TimestampParser()
                .withClock(new FixedClock(time))
                .withOutputField("processing_timestamp")
                .parse(input);
        assertEquals(FieldValue.of(Long.toString(time)), output.getField(FieldName.of("processing_timestamp")).get(),
            "Expected a timestamp to have been added to the message.");
        assertEquals(FieldValue.of("value1"), output.getField(FieldName.of("field1")).get(),
            "Expected the same input fields to be available on the output message.");
        assertFalse(output.getError().isPresent(),
            "Expected no errors to have occurred.");
    }

    @Test
    void defaultTimestampField() {
        long time = 1426349294842L;
        Message input = Message.builder()
                .addField(FieldName.of("field1"), FieldValue.of("value1"))
                .build();
        TimestampParser parser = new TimestampParser();
        Message output = parser
                .withClock(new FixedClock(time))
                .parse(input);
        FieldName defaultFieldName = parser.getOutputField();
        assertEquals(FieldValue.of(Long.toString(time)), output.getField(defaultFieldName).get(),
                "Expected a timestamp to have been added using the default field name.");
        assertEquals(FieldValue.of("value1"), output.getField(FieldName.of("field1")).get(),
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
