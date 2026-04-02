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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.apache.flink.util.Preconditions;

import java.io.Serializable;
import java.util.HashMap;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopicParserConfig implements Serializable {
    private static final String NULL_TOPIC_CONFIG = "TopicParserConfig is missing %s";
    public static final String NULL_CHAIN_KEY_CONFIG = String.format(NULL_TOPIC_CONFIG, "chainKey");
    public static final String NULL_SOURCE_CONFIG = String.format(NULL_TOPIC_CONFIG, "source");
    public static final String EMPTY_FILE_PATTERN_TO_PARSER_MAP = "TopicParserConfig filePatternToParserMao is empty.  At least one file pattern must be mapped.";
    private static final String NULL_FILE_PATTERN_TO_PARSER_MAP = "TopicParserConfig filePatternToParserMap entry has null %s.";
    public static final String NULL_KEY_FILE_PATTERN_TO_PARSER_MAP = String.format(NULL_FILE_PATTERN_TO_PARSER_MAP, "key");
    public static final String NULL_VALUE_FILE_PATTERN_TO_PARSER_MAP = String.format(NULL_FILE_PATTERN_TO_PARSER_MAP, "value");
    private String chainKey;
    private String source;
    private String broker;
    private HashMap<String, ParserChainSource> filePatternToParserMap;

    public boolean hasFileParser() {
        return (filePatternToParserMap != null);
    }

    public void validate() {
        if (!hasFileParser()) {
            Preconditions.checkArgument(StringUtils.isNotEmpty(chainKey), NULL_CHAIN_KEY_CONFIG);
            Preconditions.checkArgument(StringUtils.isNotEmpty(source), NULL_SOURCE_CONFIG);
        } else {
            Preconditions.checkArgument(!filePatternToParserMap.isEmpty(), EMPTY_FILE_PATTERN_TO_PARSER_MAP);
            filePatternToParserMap.forEach(TopicParserConfig::validateFilePatternMapEntry);
        }
    }

    private static void validateFilePatternMapEntry(String filePathPattern, ParserChainSource parserChainSource) {
        Preconditions.checkArgument(StringUtils.isNotEmpty(filePathPattern), NULL_KEY_FILE_PATTERN_TO_PARSER_MAP);
        Preconditions.checkNotNull(parserChainSource, NULL_VALUE_FILE_PATTERN_TO_PARSER_MAP);
        parserChainSource.validate(filePathPattern);
    }

}
