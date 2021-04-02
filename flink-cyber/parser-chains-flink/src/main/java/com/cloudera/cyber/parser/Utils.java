package com.cloudera.cyber.parser;

import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

public class Utils {

    public static final String DEFAULT_BROKER = "default";

    private Utils() {
    }

    public static Map<String, String> getBrokerTopicMap(TopicPatternToChainMap topicPatternToChainMap) {
        return topicPatternToChainMap.entrySet().stream()
                .collect(Collectors.groupingBy(
                        mapEntry -> StringUtils.defaultIfEmpty(mapEntry.getValue().getBroker(), DEFAULT_BROKER),
                        Collectors.mapping(
                                Entry::getKey, Collectors.joining("|"))));
    }

    public static Map<String, Pattern> getBrokerTopicPatternMap(TopicPatternToChainMap topicPatternToChainMap) {
        return getBrokerTopicMap(topicPatternToChainMap).entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, entry -> Pattern.compile(entry.getValue())));
    }
}
