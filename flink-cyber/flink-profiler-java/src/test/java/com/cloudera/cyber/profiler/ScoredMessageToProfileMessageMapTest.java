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

import com.cloudera.cyber.Message;
import com.cloudera.cyber.TestUtils;
import com.cloudera.cyber.scoring.ScoredMessage;
import com.cloudera.cyber.scoring.Scores;
import com.cloudera.cyber.scoring.ScoringProcessFunction;
import com.cloudera.cyber.scoring.ScoringSummarizationMode;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.cloudera.cyber.profiler.accumulator.ProfileGroupConfigTestUtils.createMeasurement;

public class ScoredMessageToProfileMessageMapTest {
    private static final String TEST_PROFILE_GROUP = "test_profile";
    private static final String KEY_1 = "key_1";

    @Test
    public void testScoredToProfileMessageConversion() {
        double expectedScore = 55.0;
        ProfileGroupConfig profileGroupConfig = createAllAggregationMethodProfileGroupConfig();
        Map<String, String> inputExtensions = Stream.concat(profileGroupConfig.getKeyFieldNames().stream(), profileGroupConfig.getMeasurementFieldNames().stream()).
                filter(fieldName -> !fieldName.equals(ScoredMessageToProfileMessageMap.CYBER_SCORE_FIELD)).
                collect(Collectors.toMap(k -> k, v -> v.concat("_value")));

        String notRequiredFieldName = "not required";
        inputExtensions.put(notRequiredFieldName, "should be removed");

        Message inputMessage = TestUtils.createMessage(inputExtensions);

        List<Scores> scoreDetails = Collections.singletonList(Scores.builder().reason("because").score(expectedScore).build());
        ScoredMessage inputScoredMessage = ScoringProcessFunction.scoreMessage(inputMessage, scoreDetails, ScoringSummarizationMode.DEFAULT());

        ScoredMessageToProfileMessageMap map = new ScoredMessageToProfileMessageMap(profileGroupConfig);
        ProfileMessage outputProfileMessage = map.map(inputScoredMessage);

        Assert.assertEquals(inputMessage.getTs(), outputProfileMessage.getTs());

        Map<String, String> expectedExtensions = new HashMap<>(inputExtensions);
        expectedExtensions.remove(notRequiredFieldName);
        expectedExtensions.put(ScoredMessageToProfileMessageMap.CYBER_SCORE_FIELD, Double.toString(expectedScore));
        Assert.assertEquals(expectedExtensions, outputProfileMessage.getExtensions());

    }

    private static ProfileGroupConfig createAllAggregationMethodProfileGroupConfig() {
        ArrayList<ProfileMeasurementConfig> measurements = Stream.of(ProfileAggregationMethod.values()).map(method -> {
            {
                String methodName = method.name().toLowerCase();
                String fieldName = ProfileAggregationMethod.usesFieldValue.get(method) ? methodName.concat("_field") : null;
                return createMeasurement(method, methodName.concat("_result"), fieldName, null);
            }
        }).collect(Collectors.toCollection(ArrayList::new));

        measurements.add(createMeasurement(ProfileAggregationMethod.MAX, "max_score", ScoredMessageToProfileMessageMap.CYBER_SCORE_FIELD, null));

        return ProfileGroupConfig.builder().
                profileGroupName(TEST_PROFILE_GROUP).keyFieldNames(Lists.newArrayList(KEY_1)).
                periodDuration(5L).periodDurationUnit("MINUTES").
                sources(Lists.newArrayList("ANY")).measurements(measurements).build();

    }
}
