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
