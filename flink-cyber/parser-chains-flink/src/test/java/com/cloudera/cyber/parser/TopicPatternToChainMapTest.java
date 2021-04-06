package com.cloudera.cyber.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Map;
import java.util.regex.Pattern;
import org.junit.Test;

public class TopicPatternToChainMapTest {
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
        map.put(TOPIC_NAME_1, new TopicParserConfig(CHAIN_KEY, SOURCE, BROKER_NAME_1));
        map.put(TOPIC_NAME_2, new TopicParserConfig(CHAIN_KEY, SOURCE, BROKER_NAME_1));
        map.put(TOPIC_NAME_3, new TopicParserConfig(CHAIN_KEY, SOURCE, BROKER_NAME_2));

        Map<String, String> brokerTopicMap = map.getBrokerPrefixTopicNameMap();

        assertThat(brokerTopicMap)
                .contains(entry(BROKER_NAME_1, TOPIC_NAME_1 + '|' + TOPIC_NAME_2), entry(BROKER_NAME_2, TOPIC_NAME_3));
    }

    @Test
    public void testGroupingTopicByBrokerWhenBrokerIsEmptyString() {
        String brokerNameEmpty = "";

        TopicPatternToChainMap map = new TopicPatternToChainMap();
        map.put(TOPIC_NAME_1, new TopicParserConfig(CHAIN_KEY, SOURCE, brokerNameEmpty));
        map.put(TOPIC_NAME_2, new TopicParserConfig(CHAIN_KEY, SOURCE, brokerNameEmpty));
        map.put(TOPIC_NAME_3, new TopicParserConfig(CHAIN_KEY, SOURCE, BROKER_NAME_2));

        Map<String, String> brokerTopicMap = map.getBrokerPrefixTopicNameMap();

        assertThat(brokerTopicMap)
                .contains(entry(TopicPatternToChainMap.DEFAULT_PREFIX, TOPIC_NAME_1 + '|' + TOPIC_NAME_2),
                        entry(BROKER_NAME_2, TOPIC_NAME_3));
    }

    @Test
    public void testGroupingTopicByBrokerWhenBrokerIsNull() {
        TopicPatternToChainMap map = new TopicPatternToChainMap();
        map.put(TOPIC_NAME_1, new TopicParserConfig(CHAIN_KEY, SOURCE, null));
        map.put(TOPIC_NAME_2, new TopicParserConfig(CHAIN_KEY, SOURCE, null));
        map.put(TOPIC_NAME_3, new TopicParserConfig(CHAIN_KEY, SOURCE, BROKER_NAME_2));

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
        map.put(customTopicName, new TopicParserConfig(CHAIN_KEY, SOURCE, BROKER_NAME_1));
        map.put(topicNamePattern, new TopicParserConfig(CHAIN_KEY, SOURCE, BROKER_NAME_1));
        map.put(customTopicName2, new TopicParserConfig(CHAIN_KEY, SOURCE, BROKER_NAME_2));

        Map<String, Pattern> brokerTopicPatternMap = map.getBrokerPrefixTopicPatternMap();

        assertThat(brokerTopicPatternMap.get(BROKER_NAME_1)).matches(
                p -> p.matcher(TOPIC_NAME_1).matches() && p.matcher(TOPIC_NAME_2).matches() && p
                        .matcher(customTopicName).matches() && !p.matcher("someTestStringasd123").matches() && !p
                        .matcher(customTopicName2).matches());
        assertThat(brokerTopicPatternMap.get(BROKER_NAME_2)).matches(
                p -> p.matcher(customTopicName2).matches() && !p.matcher(TOPIC_NAME_1).matches() && !p
                        .matcher(customTopicName).matches());
    }
}