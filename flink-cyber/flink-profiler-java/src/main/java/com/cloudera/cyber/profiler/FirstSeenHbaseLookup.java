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

import com.cloudera.cyber.EnrichmentEntry;
import com.cloudera.cyber.commands.CommandType;
import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageConfig;
import com.cloudera.cyber.hbase.AbstractHbaseMapFunction;
import com.cloudera.cyber.hbase.LookupKey;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class FirstSeenHbaseLookup extends AbstractHbaseMapFunction<ProfileMessage, ProfileMessage> {
    public static final String FIRST_SEEN_PROPERTY_NAME = "firstSeen";
    public static final String LAST_SEEN_PROPERTY_NAME = "lastSeen";
    public static final String FIRST_SEEN_TIME_SUFFIX = ".time";
    public static final String FIRST_SEEN_ENRICHMENT_TYPE = "first_seen";
    private FirstSeenHBase firstSeenHBaseInfo;
    private Duration firstSeenExpireDuration;
    private EnrichmentStorageConfig enrichmentStorageConfig;
    protected final OutputTag<EnrichmentCommand> firstSeenUpdateOutput = new OutputTag<EnrichmentCommand>(ProfileJob.FIRST_SEEN_ENRICHMENT_UPDATE){};


    public FirstSeenHbaseLookup(EnrichmentStorageConfig enrichmentStorageConfig, ProfileGroupConfig profileGroupConfig) throws IllegalStateException {
        this.firstSeenHBaseInfo = new FirstSeenHBase(enrichmentStorageConfig, profileGroupConfig);
        ProfileMeasurementConfig measurementConfig = profileGroupConfig.getMeasurements().stream().filter(m -> m.getAggregationMethod().equals(ProfileAggregationMethod.FIRST_SEEN)).
                findFirst().orElseThrow(() -> new NullPointerException("Expected at least one first seen measurement but none was found."));


        this.firstSeenExpireDuration = Duration.of(measurementConfig.getFirstSeenExpirationDuration(), ChronoUnit.valueOf(measurementConfig.getFirstSeenExpirationDurationUnit()));
        this.enrichmentStorageConfig = enrichmentStorageConfig;
    }

    @Override
    public void processElement(ProfileMessage message, Context context, Collector<ProfileMessage> collector) {
        messageCounter.inc(1);
        LookupKey lookupKey = firstSeenHBaseInfo.getKey(message);
        Map<String, Object> previousFirstLastSeen = fetch(lookupKey);
        boolean first = previousFirstLastSeen.isEmpty();
        Map<String, String> firstSeenExtensions = new HashMap<>();
        String firstSeen = firstSeenHBaseInfo.getFirstSeen(message);
        log.warn("Previous of {} = {}", lookupKey, previousFirstLastSeen);
        if (!first) {
            String previousFirstTimestamp = mergeFirstLastSeen((String)previousFirstLastSeen.get(FIRST_SEEN_PROPERTY_NAME),
                    (String)previousFirstLastSeen.get(LAST_SEEN_PROPERTY_NAME), firstSeenHBaseInfo.getFirstSeen(message));
            if (previousFirstTimestamp != null) {
                // there was a previous observation - send the first timestamp
                firstSeenExtensions.put(firstSeenHBaseInfo.getFirstSeenResultName().concat(FIRST_SEEN_TIME_SUFFIX), previousFirstTimestamp);
                firstSeen = previousFirstTimestamp;
            } else {
                // previous observation was too old, treat this like a new observation
                log.warn("Observation of {} was too old.  Adding new obs", lookupKey);
                first = true;
            }
        }
        firstSeenExtensions.put(firstSeenHBaseInfo.getFirstSeenResultName(), Integer.toString(first ? 1 : 0));

        message.getExtensions().forEach(firstSeenExtensions::putIfAbsent);

        collector.collect(new ProfileMessage(message.getTs(), firstSeenExtensions));
        EnrichmentCommand enrichmentCommand = createFirstSeenUpdate(message.getTs(), lookupKey, firstSeen, firstSeenHBaseInfo.getLastSeen(message));
        log.warn("New FirstSeen of {} = {}", lookupKey, enrichmentCommand);
        context.output(firstSeenUpdateOutput, enrichmentCommand);
    }

    private EnrichmentCommand createFirstSeenUpdate(long ts, LookupKey key, String firstSeen, String lastSeen) {
        Map<String, String> firstSeenMap = new HashMap<>();
        firstSeenMap.put(FirstSeenHbaseLookup.FIRST_SEEN_PROPERTY_NAME, firstSeen);
        firstSeenMap.put(FirstSeenHbaseLookup.LAST_SEEN_PROPERTY_NAME, lastSeen);

        EnrichmentEntry enrichmentEntry = EnrichmentEntry.builder().ts(ts).
                type(FIRST_SEEN_ENRICHMENT_TYPE).
                key(key.getKey()).entries(firstSeenMap).build();

        return EnrichmentCommand.builder().type(CommandType.ADD).
                headers(Collections.emptyMap()).
                payload(enrichmentEntry).build();
    }

    protected String mergeFirstLastSeen(String previousFirstString, String previousLastString, String newFirstString) {
        log.warn("Merging previousfirst={} previousLast={} with newFirst={}", previousFirstString, previousLastString, newFirstString);
        if (previousFirstString != null && previousLastString != null) {
            Instant previousFirst = parseEpochMilliTimestamp(previousFirstString);
            Instant previousLast = parseEpochMilliTimestamp(previousLastString);
            Instant newFirst = parseEpochMilliTimestamp(newFirstString);
            log.warn("Converted to instant previousfirst={} previousLast={} with newFirst={}", previousFirst, previousLast, newFirst);
            if (previousFirst != null && previousLast != null && newFirst != null) {
               Duration durationSinceLastObs = Duration.between(previousLast, newFirst);
               log.warn("DurationSinceLast = {} vs expiration = {}", humanReadableFormat(durationSinceLastObs), humanReadableFormat(firstSeenExpireDuration));
               if (durationSinceLastObs.compareTo(firstSeenExpireDuration) < 0) {
                   // previous first seen is not too old so return the minimum
                   return previousFirstString;
               }
            }
        }
        // this is the first observation or the previous observation was too old
        return null;
    }

    public static Instant parseEpochMilliTimestamp(String epochMillisString) {
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

    public static String humanReadableFormat(Duration duration) {
        return duration.toString()
                .substring(2)
                .replaceAll("(\\d[HMS])(?!$)", "$1 ")
                .toLowerCase();
    }
}
