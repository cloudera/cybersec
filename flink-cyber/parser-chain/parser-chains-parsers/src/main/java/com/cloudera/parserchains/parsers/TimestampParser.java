package com.cloudera.parserchains.parsers;

import com.cloudera.parserchains.core.FieldName;
import com.cloudera.parserchains.core.FieldValue;
import com.cloudera.parserchains.core.Message;
import com.cloudera.parserchains.core.Parser;
import com.cloudera.parserchains.core.catalog.Configurable;
import com.cloudera.parserchains.core.catalog.MessageParser;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * A parser that adds the current system time as a field to the message. Useful for
 * tracking the time when a message was parsed.
 */
@MessageParser(
    name="Timestamp",
    description="Adds a timestamp to a message. Can be used to mark processing time.")
public class TimestampParser implements Parser {
    private static final String DEFAULT_OUTPUT_FIELD = "timestamp";
    private FieldName outputField;
    private Clock clock;

    public TimestampParser() {
        this.outputField = FieldName.of(DEFAULT_OUTPUT_FIELD);
        this.clock = new Clock();
    }

    @Override
    public Message parse(Message input) {
        Long now = clock.currentTimeMillis();
        FieldValue timestamp = FieldValue.of(Long.toString(now));
        return Message.builder()
                .withFields(input)
                .addField(outputField, timestamp)
                .build();
    }

    @Configurable(key="outputField",
            label="Output Field",
            description="The field that will contain the timestamp.",
            defaultValue=DEFAULT_OUTPUT_FIELD)
    public TimestampParser withOutputField(String fieldName) {
        if(StringUtils.isNotBlank(fieldName)) {
            this.outputField = FieldName.of(fieldName);
        }
        return this;
    }

    /**
     * @param clock A {@link Clock} to use during testing.
     */
    public TimestampParser withClock(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
        return this;
    }

    public FieldName getOutputField() {
        return outputField;
    }

    /**
     * The source of the current timestamp. Enables testing.
     */
    public static class Clock {
        public long currentTimeMillis() {
            return System.currentTimeMillis();
        }
    }
}
