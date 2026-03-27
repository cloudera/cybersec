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

package com.cloudera.cyber.parser;

import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

public class TopicPatternToChainMapTest {
    public static final String ERROR_TOPIC_PATTERN = "topic_pattern";
    private final String TOPIC_NAME_1 = "topic1";
    private final String TOPIC_NAME_2 = "topic2";
    private final String TOPIC_NAME_3 = "topic3";
    private final String BROKER_NAME_1 = "broker1:8283";
    private final String BROKER_NAME_2 = "broker2:8181";
    private final String CHAIN_KEY = "chainkey";
    private final String SOURCE = "source";

    @Test
    public void testGroupingTopicByBroker() {
        TopicPatternToChainMap map = new TopicPatternToChainMap();
        map.put(TOPIC_NAME_1, new TopicParserConfig(CHAIN_KEY, SOURCE, BROKER_NAME_1, null));
        map.put(TOPIC_NAME_2, new TopicParserConfig(CHAIN_KEY, SOURCE, BROKER_NAME_1, null));
        map.put(TOPIC_NAME_3, new TopicParserConfig(CHAIN_KEY, SOURCE, BROKER_NAME_2, null));

        Map<String, String> brokerTopicMap = map.getBrokerPrefixTopicNameMap();

        assertThat(brokerTopicMap)
                .contains(entry(BROKER_NAME_1, TOPIC_NAME_1 + '|' + TOPIC_NAME_2), entry(BROKER_NAME_2, TOPIC_NAME_3));
    }

    @Test
    public void testGroupingTopicByBrokerWhenBrokerIsEmptyString() {
        String brokerNameEmpty = "";

        TopicPatternToChainMap map = new TopicPatternToChainMap();
        map.put(TOPIC_NAME_1, new TopicParserConfig(CHAIN_KEY, SOURCE, brokerNameEmpty, null));
        map.put(TOPIC_NAME_2, new TopicParserConfig(CHAIN_KEY, SOURCE, brokerNameEmpty, null));
        map.put(TOPIC_NAME_3, new TopicParserConfig(CHAIN_KEY, SOURCE, BROKER_NAME_2, null));

        Map<String, String> brokerTopicMap = map.getBrokerPrefixTopicNameMap();

        assertThat(brokerTopicMap)
                .contains(entry(TopicPatternToChainMap.DEFAULT_PREFIX, TOPIC_NAME_1 + '|' + TOPIC_NAME_2),
                        entry(BROKER_NAME_2, TOPIC_NAME_3));
    }

    @Test
    public void testGroupingTopicByBrokerWhenBrokerIsNull() {
        TopicPatternToChainMap map = new TopicPatternToChainMap();
        map.put(TOPIC_NAME_1, new TopicParserConfig(CHAIN_KEY, SOURCE, null, null));
        map.put(TOPIC_NAME_2, new TopicParserConfig(CHAIN_KEY, SOURCE, null, null));
        map.put(TOPIC_NAME_3, new TopicParserConfig(CHAIN_KEY, SOURCE, BROKER_NAME_2, null));

        Map<String, String> brokerTopicMap = map.getBrokerPrefixTopicNameMap();

        assertThat(brokerTopicMap)
                .contains(entry(TopicPatternToChainMap.DEFAULT_PREFIX, TOPIC_NAME_1 + '|' + TOPIC_NAME_2),
                        entry(BROKER_NAME_2, TOPIC_NAME_3));
    }

    @Test
    public void testGroupingTopicByBrokerAndCreateCorrectPattern() {
        String customTopicName = "custom";
        String customTopicName2 = "customTopicName2";
        String topicNamePattern = "topic.*";

        TopicPatternToChainMap map = new TopicPatternToChainMap();
        map.put(customTopicName, new TopicParserConfig(CHAIN_KEY, SOURCE, BROKER_NAME_1, null));
        map.put(topicNamePattern, new TopicParserConfig(CHAIN_KEY, SOURCE, BROKER_NAME_1, null));
        map.put(customTopicName2, new TopicParserConfig(CHAIN_KEY, SOURCE, BROKER_NAME_2, null));

        Map<String, Pattern> brokerTopicPatternMap = map.getBrokerPrefixTopicPatternMap();

        assertThat(brokerTopicPatternMap.get(BROKER_NAME_1)).matches(
                p -> p.matcher(TOPIC_NAME_1).matches() && p.matcher(TOPIC_NAME_2).matches() && p
                        .matcher(customTopicName).matches() && !p.matcher("someTestStringasd123").matches() && !p
                        .matcher(customTopicName2).matches());
        assertThat(brokerTopicPatternMap.get(BROKER_NAME_2)).matches(
                p -> p.matcher(customTopicName2).matches() && !p.matcher(TOPIC_NAME_1).matches() && !p
                        .matcher(customTopicName).matches());
    }

    @Test
    public void testGetSourcesDuplicateSourcesNoFiles() {
        TopicPatternToChainMap map = new TopicPatternToChainMap();
        map.put(TOPIC_NAME_1, new TopicParserConfig(CHAIN_KEY, SOURCE, null, null));
        map.put(TOPIC_NAME_2, new TopicParserConfig(CHAIN_KEY, SOURCE, null, null));
        map.put(TOPIC_NAME_3, new TopicParserConfig(CHAIN_KEY, SOURCE, BROKER_NAME_2, null));

        assertThat(map.getSourcesProduced()).containsOnly(SOURCE);
    }

    @Test
    public void testGetSourcesUniqueSourcesNoFiles() {
        TopicPatternToChainMap map = new TopicPatternToChainMap();
        String source1 = "source 1";
        String source2 = "source 2";
        String source3 = "source 3";

        map.put(TOPIC_NAME_1, new TopicParserConfig(CHAIN_KEY, source1, null, null));
        map.put(TOPIC_NAME_2, new TopicParserConfig(CHAIN_KEY, source2, null, null));
        map.put(TOPIC_NAME_3, new TopicParserConfig(CHAIN_KEY, source3, BROKER_NAME_2, null));

        assertThat(map.getSourcesProduced()).containsOnly(source1, source2, source3);
    }

    @Test
    public void testEmptyMap() {
        TopicPatternToChainMap map = new TopicPatternToChainMap();
        assertThatCode(map::validate).doesNotThrowAnyException();
    }

    @Test
    public void testNullTopicPattern() {
        TopicPatternToChainMap map = new TopicPatternToChainMap();
        map.put(null, new TopicParserConfig(CHAIN_KEY, "source1", null, null));
        assertThatThrownBy(map::validate).isInstanceOf(IllegalArgumentException.class).hasMessage(TopicPatternToChainMap.TOPIC_MAP_HAS_NULL_TOPIC_PATTERN_MESSAGE);
    }

    @Test
    public void testNullParserConfig() {
        assertThatThrownBy(() -> validateMapWithError(null)).isInstanceOf(NullPointerException.class).hasMessage(String.format(TopicPatternToChainMap.TOPIC_MAP_HAS_NULL_PARSER_CONFIG_MESSAGE, ERROR_TOPIC_PATTERN));
    }

    @Test
    public void testInvalidParserConfig() {
        assertThatThrownBy(() -> validateMapWithError(new TopicParserConfig(CHAIN_KEY, null, null, null))).isInstanceOf(IllegalArgumentException.class).hasMessage(TopicParserConfig.NULL_SOURCE_CONFIG);
    }

    private static void validateMapWithError(TopicParserConfig parserConfig) {
        TopicPatternToChainMap map = new TopicPatternToChainMap();
        map.put(ERROR_TOPIC_PATTERN, parserConfig);
        map.validate();
    }

}