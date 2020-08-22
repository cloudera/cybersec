package com.cloudera.parserchains.parsers;

import com.cloudera.parserchains.core.FieldName;
import com.cloudera.parserchains.core.FieldValue;
import com.cloudera.parserchains.core.Message;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class TimestampFormatParserTest {

    @Test
    public void testSingleField() {
        Message input = Message.builder()
                .addField(FieldName.of("time"), FieldValue.of("2020-08-21T18:51:12.345Z"))
                .build();
        Message output = new TimestampFormatParser()
                .withOutputField("time", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "UTC")
                .parse(input);

        assertThat("Time is correct", output.getField(FieldName.of("time")).get(), equalTo(FieldValue.of("1598035872345")));
    }

    @Test
    public void testMultipleFields() {
        Message input = Message.builder()
                .addField(FieldName.of("time"), FieldValue.of("2020-08-21T18:51:12.345Z"))
                .addField(FieldName.of("time2"), FieldValue.of("2020-07-21T12:34:56.789Z"))
                .build();
        Message output = new TimestampFormatParser()
                .withOutputField("time", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "UTC")
                .withOutputField("time2", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "UTC")
                .parse(input);

        assertThat("Time is correct", output.getField(FieldName.of("time")).get(), equalTo(FieldValue.of("1598035872345")));
        assertThat("Time is correct", output.getField(FieldName.of("time2")).get(), equalTo(FieldValue.of("1595334896789")));
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
