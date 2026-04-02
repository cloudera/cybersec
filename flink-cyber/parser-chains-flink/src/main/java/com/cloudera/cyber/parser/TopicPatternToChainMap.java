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

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.util.Preconditions;

public class TopicPatternToChainMap extends HashMap<String, TopicParserConfig> {

    public static final String DEFAULT_PREFIX = "default";
    public static final String TOPIC_MAP_HAS_NULL_TOPIC_PATTERN_MESSAGE = "Topic map has null topic pattern.";
    public static final String TOPIC_MAP_HAS_NULL_PARSER_CONFIG_MESSAGE = "Topic map %s has null parser config.";

    public void validate() {
        // by default a topic name maps to a chain
        if (!isEmpty()) {
            forEach(this::validateTopicMappingEntry);
        }
    }

    private void validateTopicMappingEntry(String topicPattern, TopicParserConfig topicParserConfig) {
        Preconditions.checkArgument(StringUtils.isNotEmpty(topicPattern), TOPIC_MAP_HAS_NULL_TOPIC_PATTERN_MESSAGE);
        Preconditions.checkNotNull(topicParserConfig, TOPIC_MAP_HAS_NULL_PARSER_CONFIG_MESSAGE, topicPattern);
        topicParserConfig.validate();
    }

    public boolean hasFileParser() {
        return this.entrySet().stream().anyMatch(e -> e.getValue().hasFileParser());
    }

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
        List<String> kafkaSources = values().stream().map(TopicParserConfig::getSource).collect(Collectors.toList());

        List<String> fileSources =
        values().stream().
                map(TopicParserConfig::getFilePatternToParserMap).
                filter(Objects::nonNull).
                flatMap(m -> m.values().stream()).
                map(ParserChainSource::getSource).collect(Collectors.toList());

        return Stream.concat(kafkaSources.stream(), fileSources.stream()).distinct().collect(Collectors.toList());
    }
}
