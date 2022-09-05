package com.cloudera.parserchains.parsers;

import com.cloudera.parserchains.core.*;
import com.cloudera.parserchains.core.catalog.Configurable;
import com.cloudera.parserchains.core.catalog.MessageParser;
import com.cloudera.parserchains.core.catalog.Parameter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * A parser to extract formatted timestamps and express them as epoch time, preserving the
 * timezone of the original in a complementary field
 */
@MessageParser(
        name = "TimestampFormat",
        description = "Parse a formatted timestamp into usable unix epoch time")
public class TimestampFormatParser implements Parser {
    private final List<String> DEFAULT_TIMEFORMAT = Arrays.asList("yyyyMMddThh:mm:ss.sssZ");
    private static final String DEFAULT_TIMEZONE = TimeZone.getDefault().getID();
    private List<Config> fields = new ArrayList<>();

    @Override
    public Message parse(Message input) {
        Message.Builder builder = Message.builder()
                .withFields(input);
        for (Config c : fields) {
            FieldName fieldName = FieldName.of(c.fieldName);
            String inputValue = input.getField(fieldName).get().get();
            // try all the date formatters until one works
            Iterator<DateTimeFormatter> i = c.dateTimeFormatter.listIterator();
            while (i.hasNext()) {
                DateTimeFormatter formatter = i.next();
                try {
                    FieldValue value = StringFieldValue.of(parseDate(inputValue, formatter, c.tz).toString());
                    builder.addField(fieldName, value);
                    break;
                } catch (DateTimeParseException e) {
                    // if this is the last one, throw it.
                    if (!i.hasNext()) throw (e);
                }
            }
        }
        return builder.build();
    }

    private Long parseDate(String inputValue, DateTimeFormatter format, String tz) {
        TemporalAccessor parse = format.withZone(ZoneId.of(tz)).parse(inputValue);
        return Instant.from(parse).toEpochMilli();
    }

    @Configurable(key = "fields",
            label = "Time Fields",
            description = "The field that will contain the timestamp.")
    public TimestampFormatParser withOutputField(
            @Parameter(key = "field", label = "Input Field", description = "Field to be parsed", required = true) String fieldName,
            @Parameter(key = "format", label = "Time format", description = "A compatible time format", required = true) String format,
            @Parameter(key = "tz", label = "Timezome", description = "Optionally set the expected timezone", required = true) String tz
    ) {

        List<String> formats = Arrays.asList(format.split(","));

        if (formats == null)
            formats = DEFAULT_TIMEFORMAT;

        if (StringUtils.isNotBlank(fieldName)) {
            this.fields.add(new TimestampFormatParser.Config(fieldName,
                    formats.stream().map(f -> DateTimeFormatter.ofPattern(f)).collect(toList()),
                    StringUtils.isNotBlank(tz) ? tz : DEFAULT_TIMEZONE));
        }
        return this;
    }

    public class Config implements Serializable {
        private String fieldName;
        protected List<DateTimeFormatter> dateTimeFormatter;
        private String tz;

        public Config(String fieldName, List<DateTimeFormatter> dateTimeFormatter, String tz) {
            this.fieldName = fieldName;
            this.dateTimeFormatter = dateTimeFormatter;
            this.tz = tz;
        }
    }
}
