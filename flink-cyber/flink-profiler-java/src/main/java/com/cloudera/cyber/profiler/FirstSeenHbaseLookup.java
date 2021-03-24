package com.cloudera.cyber.profiler;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.hbase.AbstractHbaseMapFunction;
import com.cloudera.cyber.hbase.LookupKey;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class FirstSeenHbaseLookup extends AbstractHbaseMapFunction {
    public static final String FIRST_SEEN_PROPERTY_NAME = "firstSeen";
    public static final String LAST_SEEN_PROPERTY_NAME = "lastSeen";
    public static final String FIRST_SEEN_TIME_SUFFIX = ".time";
    private FirstSeenHBase firstSeenHBaseInfo;
    private Duration firstSeenExpireDuration;

    public FirstSeenHbaseLookup(String tableName, String columnFamilyName, ProfileGroupConfig profileGroupConfig) throws IllegalStateException {
        this.firstSeenHBaseInfo = new FirstSeenHBase(tableName, columnFamilyName, profileGroupConfig);
        ProfileMeasurementConfig measurementConfig = profileGroupConfig.getMeasurements().stream().filter(m -> m.getAggregationMethod().equals(ProfileAggregationMethod.FIRST_SEEN)).
                findFirst().orElseThrow(() -> new NullPointerException("Expected at least one first seen measurement but none was found."));

        this.firstSeenExpireDuration = Duration.of(measurementConfig.getFirstSeenExpirationDuration(), ChronoUnit.valueOf(profileGroupConfig.getPeriodDurationUnit()));
    }

    @Override
    protected String getTableName() {
        return firstSeenHBaseInfo.getTableName();
    }

    @Override
    public Message map(Message message) {
        messageCounter.inc(1);
        LookupKey lookupKey = firstSeenHBaseInfo.getKey(message);
        Map<String, String> previousFirstLastSeen = fetch(lookupKey);
        boolean first = previousFirstLastSeen.isEmpty();
        Map<String, String> firstSeenExtensions = new HashMap<>();
        if (!first) {
            String previousFirstTimestamp = mergeFirstLastSeen(previousFirstLastSeen.get(FIRST_SEEN_PROPERTY_NAME),
                    previousFirstLastSeen.get(LAST_SEEN_PROPERTY_NAME), firstSeenHBaseInfo.getFirstSeen(message));
            if (previousFirstTimestamp != null) {
                // there was a previous observation - send the first timestamp
                firstSeenExtensions.put(firstSeenHBaseInfo.getFirstSeenResultName().concat(FIRST_SEEN_TIME_SUFFIX), previousFirstTimestamp);
            } else {
                // previous observation was too old, treat this like a new observation
                first = true;
            }
        }
        firstSeenExtensions.put(firstSeenHBaseInfo.getFirstSeenResultName(), Integer.toString(first ? 1 : 0));
        return MessageUtils.addFields(message, firstSeenExtensions);
    }

    protected String mergeFirstLastSeen(String previousFirstString, String previousLastString, String newFirstString) {
        if (previousFirstString != null && previousLastString != null) {
            Instant previousFirst = parseEpochMilliTimestamp(previousFirstString);
            Instant previousLast = parseEpochMilliTimestamp(previousLastString);
            Instant newFirst = parseEpochMilliTimestamp(newFirstString);
            if (previousFirst != null && previousLast != null && newFirst != null) {
               Duration durationSinceLastObs = Duration.between(previousLast, newFirst);
               if (durationSinceLastObs.compareTo(firstSeenExpireDuration) < 0) {
                   // previous first seen is not too old so return the minimum
                   return previousFirstString;
               }
            }
        }
        // this is the first observation or the previous observation was too old
        return null;
    }

    private static Instant parseEpochMilliTimestamp(String epochMillisString) {
        try {
            if (epochMillisString != null) {
                long epochMillis = Long.parseLong(epochMillisString);
                return Instant.ofEpochMilli(epochMillis);
            }
        } catch (NumberFormatException e) {
           log.error(String.format("Unable to parse Epoch millis string %s", epochMillisString), e);
        }
        return null;
    }
}
