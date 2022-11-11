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

package com.cloudera.cyber.profiler;

import com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageConfig;
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
public class FirstSeenHbaseLookup extends AbstractHbaseMapFunction<ProfileMessage, ProfileMessage> {
    public static final String FIRST_SEEN_PROPERTY_NAME = "firstSeen";
    public static final String LAST_SEEN_PROPERTY_NAME = "lastSeen";
    public static final String FIRST_SEEN_TIME_SUFFIX = ".time";
    private FirstSeenHBase firstSeenHBaseInfo;
    private Duration firstSeenExpireDuration;
    private EnrichmentStorageConfig enrichmentStorageConfig;

    public FirstSeenHbaseLookup(EnrichmentStorageConfig enrichmentStorageConfig, ProfileGroupConfig profileGroupConfig) throws IllegalStateException {
        this.firstSeenHBaseInfo = new FirstSeenHBase(enrichmentStorageConfig, profileGroupConfig);
        ProfileMeasurementConfig measurementConfig = profileGroupConfig.getMeasurements().stream().filter(m -> m.getAggregationMethod().equals(ProfileAggregationMethod.FIRST_SEEN)).
                findFirst().orElseThrow(() -> new NullPointerException("Expected at least one first seen measurement but none was found."));

        this.firstSeenExpireDuration = Duration.of(measurementConfig.getFirstSeenExpirationDuration(), ChronoUnit.valueOf(profileGroupConfig.getPeriodDurationUnit()));
        this.enrichmentStorageConfig = enrichmentStorageConfig;
    }

    @Override
    public ProfileMessage map(ProfileMessage message) {
        messageCounter.inc(1);
        LookupKey lookupKey = firstSeenHBaseInfo.getKey(message);
        Map<String, Object> previousFirstLastSeen = fetch(lookupKey);
        boolean first = previousFirstLastSeen.isEmpty();
        Map<String, String> firstSeenExtensions = new HashMap<>();
        if (!first) {
            String previousFirstTimestamp = mergeFirstLastSeen((String)previousFirstLastSeen.get(FIRST_SEEN_PROPERTY_NAME),
                    (String)previousFirstLastSeen.get(LAST_SEEN_PROPERTY_NAME), firstSeenHBaseInfo.getFirstSeen(message));
            if (previousFirstTimestamp != null) {
                // there was a previous observation - send the first timestamp
                firstSeenExtensions.put(firstSeenHBaseInfo.getFirstSeenResultName().concat(FIRST_SEEN_TIME_SUFFIX), previousFirstTimestamp);
            } else {
                // previous observation was too old, treat this like a new observation
                first = true;
            }
        }
        firstSeenExtensions.put(firstSeenHBaseInfo.getFirstSeenResultName(), Integer.toString(first ? 1 : 0));

        message.getExtensions().forEach(firstSeenExtensions::putIfAbsent);

        return new ProfileMessage(message.getTs(), firstSeenExtensions);
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
