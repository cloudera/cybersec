package com.cloudera.cyber.indexing.hive;

import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class TimestampNormalizer implements Function<String, Object> {
    private List<SimpleDateFormat> possibleTimestampFormats;
    private SimpleDateFormat normalizedFormat;

    public TimestampNormalizer(String possibleSourceTimestampFormatCSV, SimpleDateFormat normalizedFormat) {
        this.possibleTimestampFormats = Stream.of(possibleSourceTimestampFormatCSV.split(",")).
                map(SimpleDateFormat::new).collect(Collectors.toList());
        this.normalizedFormat = normalizedFormat;
    }

    @Override
    public Object apply(String fieldValue) {
        Date parsedDate = tryEpochMillis(fieldValue);
        if (parsedDate == null) {
            for(SimpleDateFormat nextFormat : possibleTimestampFormats) {
                try {
                    parsedDate = nextFormat.parse(fieldValue);
                    break;
                } catch (ParseException e) {
                    log.debug("Timestamp {} does not match format {}.", fieldValue, nextFormat);
                }
            }
        }
        if (parsedDate != null) {
            return normalizedFormat.format(parsedDate);
        } else {
            throw new IllegalStateException(String.format("Field value '%s' can't be normalized to a timestamp because it does not match any configured format.", fieldValue));
        }
    }

    private Date tryEpochMillis(String fieldValue) {
        try {
            return Date.from(Instant.ofEpochMilli(Long.parseLong(fieldValue)));
        } catch(NumberFormatException numberFormatException) {
            log.debug("Timestamp {} is not a long epoch millis.", fieldValue);
            // not a long, go on to the next possible formats.
        }
        return null;
    }

}
