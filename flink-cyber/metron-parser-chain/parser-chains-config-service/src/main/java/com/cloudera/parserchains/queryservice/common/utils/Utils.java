package com.cloudera.parserchains.queryservice.common.utils;

import org.apache.commons.lang.StringUtils;

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
import java.util.Comparator;
import java.util.List;


public class Utils {

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

    public static List<SimpleDateFormat> DATE_FORMATS = new ArrayList<SimpleDateFormat>() {
        {
            add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ"));

            // common format per rsyslog defaults e.g. Mar 21 14:05:02
            add(new SimpleDateFormat("MMM dd HH:mm:ss"));
            add(new SimpleDateFormat("MMM dd yyyy HH:mm:ss"));

            // additional formats found in the wild
            add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
            add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ"));
            add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"));

        }
    };


    /**
     * Parse the data according to a sequence of possible parse patterns
     * If the given date is entirely numeric, it is assumed to be a unix
     * timestamp.
     * If the year is not specified in the date string, use the current year.
     * Assume that any date more than 4 days in the future is in the past as per
     * SyslogUtils
     *
     * @param candidate
     *            The possible date.
     * @param validPatterns
     *            A list of SimpleDateFormat instances to try parsing with.
     * @return A java.util.Date based on the parse result
     */
    public static Long parseData(String candidate, List<SimpleDateFormat> validPatterns) {
        if (StringUtils.isNumeric(candidate)) {
            return Long.parseLong(candidate);
        } else {
            for (SimpleDateFormat pattern : validPatterns) {
                try {
                    DateTimeFormatterBuilder formatterBuilder = new DateTimeFormatterBuilder()
                            .appendPattern(pattern.toPattern());
                    DateTimeFormatter formatter = formatterBuilder.toFormatter();
                    ZonedDateTime parsedValue = parseDateTimeWithDefaultTimezone(candidate, formatter);
                    return parsedValue.toInstant().toEpochMilli();
                } catch (DateTimeParseException ignored) {
                    // Continue to the next pattern
                }
            }
          return null;
        }
    }

    private static ZonedDateTime parseDateTimeWithDefaultTimezone(String candidate, DateTimeFormatter formatter) {
        TemporalAccessor temporalAccessor = formatter.parseBest(candidate, ZonedDateTime::from, LocalDateTime::from);
        return temporalAccessor instanceof ZonedDateTime
                ? ((ZonedDateTime) temporalAccessor)
                : ((LocalDateTime) temporalAccessor).atZone(ZoneId.systemDefault());
    }

    public static int compareLongs(Long a, Long b) {
        Comparator<Long> comparator = Comparator.nullsFirst(Long::compareTo);
        return comparator.compare(a, b);
    }
}
