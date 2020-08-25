package com.cloudera.parserchains.parsers;

import com.cloudera.parserchains.core.FieldName;
import com.cloudera.parserchains.core.FieldValue;
import com.cloudera.parserchains.core.Message;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

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
    public void testFallBackFormat() {

        Message input = Message.builder()
                .addField(FieldName.of("time"), FieldValue.of("2020-08-21T18:51:12Z"))
                .build();
        Message output = new TimestampFormatParser()
                .withOutputField("time", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z',yyyy-MM-dd'T'HH:mm:ss'Z'", "UTC")
                .parse(input);

        assertThat("Time is correct", output.getField(FieldName.of("time")).get(), equalTo(FieldValue.of("1598035872000")));
    }

    @Test
    public void testWildCases() {
        run("2020-08-24T04:40:11.752Z", "1598244011752");
        run("2020-08-24T04:40:11.7Z", "1598244011700");
    }


    private Message run(String in, String expected) {
        Message input = Message.builder()
                .addField(FieldName.of("time"), FieldValue.of(in))
                .build();

        Message output = new TimestampFormatParser()
                .withOutputField("time", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z',yyyy-MM-dd'T'HH:mm:ss.SS'Z',yyyy-MM-dd'T'HH:mm:ss.S'Z',yyyy-MM-dd'T'HH:mm:ss'Z'", "UTC")
                .parse(input);

        assertThat("Time is correct", output.getField(FieldName.of("time")).get(), equalTo(FieldValue.of(expected)));
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
