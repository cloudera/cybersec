/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.metron.parsers.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;

/**
 * Various utilities for parsing and extracting dates.
 */
public class DateUtils {

    // Per IBM LEEF guide at https://www.ibm.com/support/knowledgecenter/SS42VS_DSM/c_LEEF_Format_Guide_intro.html
    public static List<SimpleDateFormat> DATE_FORMATS_LEEF = new ArrayList<SimpleDateFormat>() {
        {
            add(new SimpleDateFormat("MMM d yyyy HH:mm:ss.SSS Z"));
            add(new SimpleDateFormat("MMM d yyyy HH:mm:ss.SSS z"));
            add(new SimpleDateFormat("MMM d yyyy HH:mm:ss.SSS"));
            add(new SimpleDateFormat("MMM d yyyy HH:mm:ss"));
        }
    };

    public static List<SimpleDateFormat> DATE_FORMATS_CEF = new ArrayList<SimpleDateFormat>() {
        {
            // as per CEF Spec
            add(new SimpleDateFormat("MMM d HH:mm:ss.SSS Z"));
            add(new SimpleDateFormat("MMM d HH:mm:ss.SSS z"));
            add(new SimpleDateFormat("MMM d HH:mm:ss.SSS"));
            add(new SimpleDateFormat("MMM d HH:mm:ss zzz"));
            add(new SimpleDateFormat("MMM d HH:mm:ss"));
            add(new SimpleDateFormat("MMM d yyyy HH:mm:ss.SSS Z"));
            add(new SimpleDateFormat("MMM d yyyy HH:mm:ss.SSS z"));
            add(new SimpleDateFormat("MMM d yyyy HH:mm:ss.SSS"));
            add(new SimpleDateFormat("MMM d yyyy HH:mm:ss Z"));
            add(new SimpleDateFormat("MMM d yyyy HH:mm:ss z"));
            add(new SimpleDateFormat("MMM d yyyy HH:mm:ss"));
            // found in the wild
            add(new SimpleDateFormat("d MMMM yyyy HH:mm:ss"));
        }
    };

    public static List<SimpleDateFormat> DATE_FORMATS_SYSLOG = new ArrayList<SimpleDateFormat>() {
        {
            // As specified in https://tools.ietf.org/html/rfc5424
            add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));

            // common format per rsyslog defaults e.g. Mar 21 14:05:02
            add(new SimpleDateFormat("MMM dd HH:mm:ss"));
            add(new SimpleDateFormat("MMM dd yyyy HH:mm:ss"));

            // additional formats found in the wild
            add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"));
            add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"));
            add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"));

        }
    };

    @SuppressWarnings("checkstyle:MemberName")
    Pattern NUMERIC = Pattern.compile("\\b\\d+\\b");

    /**
     * Parse the data according to a sequence of possible parse patterns.
     *
     * <p>
     * If the given date is entirely numeric, it is assumed to be a unix timestamp.
     *
     * <p>
     * If the year is not specified in the date string, use the current year.
     * Assume that any date more than 4 days in the future is in the past as per
     * SyslogUtils
     *
     * @param candidate     The possible date.
     * @param validPatterns A list of SimpleDateFormat instances to try parsing with.
     * @return A java.util.Date based on the parse result
     */
    public static long parseMultiformat(String candidate, List<SimpleDateFormat> validPatterns) throws ParseException {
        if (StringUtils.isNumeric(candidate)) {
            return Long.valueOf(candidate);
        } else {
            for (SimpleDateFormat pattern : validPatterns) {
                try {
                    DateTimeFormatterBuilder formatterBuilder = new DateTimeFormatterBuilder()
                          .appendPattern(pattern.toPattern());
                    if (!pattern.toPattern().contains("y")) {
                        formatterBuilder
                              .parseDefaulting(ChronoField.YEAR, LocalDate.now().getYear());
                    }
                    DateTimeFormatter formatter = formatterBuilder.toFormatter();
                    ZonedDateTime parsedValue = parseDateTimeWithDefaultTimezone(candidate, formatter);
                    ZonedDateTime current = ZonedDateTime.now(parsedValue.getZone());

                    current = current.plusDays(4);
                    if (parsedValue.isAfter(current)) {
                        parsedValue = parsedValue.minusYears(1);
                    }
                    return parsedValue.toInstant().toEpochMilli();
                } catch (DateTimeParseException e) {
                    continue;
                }
            }
            throw new ParseException("Failed to parse any of the given date formats", 0);
        }
    }

    private static ZonedDateTime parseDateTimeWithDefaultTimezone(String candidate, DateTimeFormatter formatter) {
        TemporalAccessor temporalAccessor = formatter.parseBest(candidate, ZonedDateTime::from, LocalDateTime::from);
        return temporalAccessor instanceof ZonedDateTime
              ? ((ZonedDateTime) temporalAccessor)
              : ((LocalDateTime) temporalAccessor).atZone(ZoneId.systemDefault());
    }
}
