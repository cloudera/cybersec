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

import static java.util.stream.Collectors.toList;

import com.cloudera.parserchains.core.FieldName;
import com.cloudera.parserchains.core.FieldValue;
import com.cloudera.parserchains.core.Message;
import com.cloudera.parserchains.core.Parser;
import com.cloudera.parserchains.core.StringFieldValue;
import com.cloudera.parserchains.core.catalog.Configurable;
import com.cloudera.parserchains.core.catalog.MessageParser;
import com.cloudera.parserchains.core.catalog.Parameter;
import java.io.Serializable;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import org.apache.commons.lang3.StringUtils;

/**
 * A parser to extract formatted timestamps and express them as epoch time, preserving the
 * timezone of the original in a complementary field.
 */
@MessageParser(
      name = "TimestampFormat",
      description = "Parse a formatted timestamp into usable unix epoch time")
public class TimestampFormatParser implements Parser {
    private static final String DEFAULT_TIMEFORMAT = "yyyyMMdd'T'hh:mm:ss.SSS'Z'";
    private static final String DEFAULT_TIMEZONE = TimeZone.getDefault().getID();
    private final List<Config> fields = new ArrayList<>();

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
                } catch (DateTimeException e) {
                    // if this is the last one, throw it.
                    if (!i.hasNext()) {
                        return builder.withError(e).build();
                    }
                }
            }
        }
        return builder.build();
    }

    private Long parseDate(String inputValue, DateTimeFormatter format, String tz) throws DateTimeException {
        TemporalAccessor parse = format.withZone(ZoneId.of(tz)).parse(inputValue);
        return Instant.from(parse).toEpochMilli();
    }

    @Configurable(key = "fields",
          label = "Time Fields",
          description = "The field that will contain the timestamp.",
          multipleValues = true)
    public TimestampFormatParser withOutputField(
          @Parameter(key = "field", label = "Input Field", description = "Field to be parsed", required = true, isOutputName = true)
          String fieldName,
          @Parameter(key = "format", label = "Time format", description = "A compatible time format", required = true)
          String format,
          @Parameter(key = "tz", label = "Timezone", description = "Optionally set the expected timezone", required = true)
          String tz
    ) {

        List<String> formats = Arrays.asList(format.split(","));

        if (formats == null) {
            formats = Collections.singletonList(DEFAULT_TIMEFORMAT);
        }

        if (StringUtils.isNotBlank(fieldName)) {
            this.fields.add(new TimestampFormatParser.Config(fieldName,
                  formats.stream().map(f -> DateTimeFormatter.ofPattern(f)).collect(toList()),
                  StringUtils.isNotBlank(tz) ? tz : DEFAULT_TIMEZONE));
        }
        return this;
    }

    public class Config implements Serializable {
        private final String fieldName;
        protected List<DateTimeFormatter> dateTimeFormatter;
        private final String tz;

        public Config(String fieldName, List<DateTimeFormatter> dateTimeFormatter, String tz) {
            this.fieldName = fieldName;
            this.dateTimeFormatter = dateTimeFormatter;
            this.tz = tz;
        }
    }
}
