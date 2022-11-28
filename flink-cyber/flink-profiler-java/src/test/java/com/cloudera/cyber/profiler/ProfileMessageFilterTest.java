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

import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.TestUtils;
import com.cloudera.cyber.scoring.ScoredMessage;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.cloudera.cyber.profiler.ScoredMessageToProfileMessageMap.CYBER_SCORE_FIELD;
import static com.cloudera.cyber.profiler.accumulator.ProfileGroupConfigTestUtils.createMeasurement;

public class ProfileMessageFilterTest {

    private static final String TEST_PROFILE_GROUP = "test_group";
    private static final String KEY_1 = "key_1";
    private static final String KEY_2 = "key_2";

    @Test
    public void testAnySourceFilter() {
        testMessageFilter("ANY", true, false);
        testMessageFilter("ANY", false, true);
        testMessageFilter("ANY", false, false);
    }

    @Test
    public void testSpecificSourceFilter() {
        testMessageFilter("message_source", true, false);
        testMessageFilter("message_source", false, true);
        testMessageFilter("message_source", false, false);
    }

    private void testMessageFilter(String source, boolean includeNullFieldMeasurements, boolean includeScoreMeasurement) {
        ProfileGroupConfig profileGroupConfig = createProfileGroupConfig(Lists.newArrayList(source),includeNullFieldMeasurements, includeScoreMeasurement);
        ProfileMessageFilter filter = new ProfileMessageFilter(profileGroupConfig);

        Map<String, String> keyFieldValues = profileGroupConfig.getKeyFieldNames().stream().
                collect(Collectors.toMap(keyFieldName -> keyFieldName, value ->"value"));
        String requiredMeasurementFieldName = profileGroupConfig.getMeasurementFieldNames().stream().filter(name -> !name.equals(CYBER_SCORE_FIELD)).
                findFirst().orElseThrow(() -> new RuntimeException("test should contain at least one required field"));

        Map<String, String> allFields = new HashMap<>(keyFieldValues);
        allFields.put(requiredMeasurementFieldName, "value");

        // source, key, and measurement fields present
        verifyFilter(filter, source, allFields, true);
        // source and key match, no measurement fields - will match if any of the measurements don't require a field
        // or are always present (score)
        verifyFilter(filter, source, keyFieldValues, includeNullFieldMeasurements || includeScoreMeasurement);

        // source does not match but key and measurement fields are present - will match if source is ANY
        verifyFilter(filter, "not the source", allFields, source.equals("ANY"));

        // source matches but key fields are not present
        verifyFilter(filter, source, null, false);
        verifyFilter(filter, source, Collections.emptyMap(), false);
        // only one of the key fields is present
        verifyFilter(filter, source, ImmutableMap.of(KEY_1, "abc"), false);
    }

    private void verifyFilter(ProfileMessageFilter filter, String source, Map<String, String> fieldValues, boolean matches) {
        ScoredMessage scoredMessage = ScoredMessage.builder().cyberScoresDetails(Collections.emptyList()).
                message(TestUtils.createMessage(MessageUtils.getCurrentTimestamp(), source, fieldValues)).build();
        Assert.assertEquals(matches, filter.filter(scoredMessage));
    }

    private static ProfileGroupConfig createProfileGroupConfig(ArrayList<String> sources, boolean includeNullFieldMeasurements, boolean includeScoreMeasurement) {
        ArrayList<ProfileMeasurementConfig> measurements = Stream.of(ProfileAggregationMethod.values()).map(method -> {
            {
                String methodName = method.name().toLowerCase();
                boolean usesFieldValue = ProfileAggregationMethod.usesFieldValue.get(method);
                if (includeNullFieldMeasurements || usesFieldValue) {
                    String fieldName = ProfileAggregationMethod.usesFieldValue.get(method) ? methodName.concat("_field") : null;
                    return createMeasurement(method, methodName.concat("_result"), fieldName, null);
                } else {
                    return null;
                }
            }
        }).filter(Objects::nonNull).collect(Collectors.toCollection(ArrayList::new));

        if (includeScoreMeasurement) {
            measurements.add(createMeasurement(ProfileAggregationMethod.SUM, "total_score", CYBER_SCORE_FIELD, null));
        }

        return ProfileGroupConfig.builder().
                profileGroupName(TEST_PROFILE_GROUP).keyFieldNames(Lists.newArrayList(KEY_1, KEY_2)).
                periodDuration(5L).periodDurationUnit("MINUTES").
                sources(sources).measurements(measurements).build();

    }
}
