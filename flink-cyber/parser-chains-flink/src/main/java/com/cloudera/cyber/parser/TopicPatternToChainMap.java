package com.cloudera.cyber.parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

public class TopicPatternToChainMap extends HashMap<String, TopicParserConfig> {

    public static final String DEFAULT_PREFIX = "default";


    public Map<String, String> getBrokerPrefixTopicNameMap() {
        return this.entrySet().stream()
                .collect(Collectors.groupingBy(
                        mapEntry -> StringUtils.defaultIfEmpty(mapEntry.getValue().getBroker(), DEFAULT_PREFIX),
                        Collectors.mapping(
                                Entry::getKey, Collectors.joining("|"))));
    }

    public Map<String, Pattern> getBrokerPrefixTopicPatternMap() {
        return getBrokerPrefixTopicNameMap().entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, entry -> Pattern.compile(entry.getValue())));
    }

    public List<String> getSourcesProduced() {
        return values().stream().map(TopicParserConfig::getSource).collect(Collectors.toList());
    }
}
