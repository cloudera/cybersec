package com.cloudera.cyber.parser;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TopicParserConfigTest {

    public static final String TEST_BROKER = "my_broker";
    public static final String TEST_CHAIN = "my_chain";
    public static final String TEST_SOURCE = "my_source";

    @Test
    public void testValidConfig() {

        // topic with raw events on default broker
        testValidRawEventsConfig(null);

        // test with raw events on specific broker
        testValidRawEventsConfig(TEST_BROKER);

        // topic with file patterns on default broker
        testValidFilePatterns(null);

        // topic with file patterns on specific broker
        testValidFilePatterns(TEST_BROKER);
    }

    private void testValidRawEventsConfig(String broker) {
        TopicParserConfig topicParserConfig = new TopicParserConfig(TEST_CHAIN,TEST_SOURCE, broker, null);
        assertThatCode(topicParserConfig::validate).doesNotThrowAnyException();
    }

    private void testValidFilePatterns(String broker) {
        HashMap<String, ParserChainSource> filePatternConfig = new HashMap<>();
        filePatternConfig.put("*/log_file_name/*", new ParserChainSource(TEST_CHAIN, TEST_SOURCE));
        com.cloudera.cyber.parser.TopicParserConfig topicParserConfig = new TopicParserConfig(null, null, broker, filePatternConfig);
        assertThatCode(topicParserConfig::validate).doesNotThrowAnyException();
    }

    @Test
    public void testInvalidConfigs() {

        // test raw events nulls
        testInvalidRawEventsConfig(TEST_CHAIN, null, TopicParserConfig.NULL_SOURCE_CONFIG);
        testInvalidRawEventsConfig(TEST_CHAIN, "", TopicParserConfig.NULL_SOURCE_CONFIG);
        testInvalidRawEventsConfig(null, TEST_SOURCE, TopicParserConfig.NULL_CHAIN_KEY_CONFIG);
        testInvalidRawEventsConfig("", TEST_SOURCE, TopicParserConfig.NULL_CHAIN_KEY_CONFIG);
        testInvalidRawEventsConfig(null, null, TopicParserConfig.NULL_CHAIN_KEY_CONFIG);
        testInvalidRawEventsConfig("", "", TopicParserConfig.NULL_CHAIN_KEY_CONFIG);

        // test file patterns and map elements
        testInvalidFilePatternMapConfig(new HashMap<>(), IllegalArgumentException.class, TopicParserConfig.EMPTY_FILE_PATTERN_TO_PARSER_MAP);

        // test one bad file map value to make sure the validation is called
        HashMap<String, ParserChainSource>  nullMapping = new HashMap<>();
        nullMapping.put("file_pattern", null);
        testInvalidFilePatternMapConfig(nullMapping, NullPointerException.class, TopicParserConfig.NULL_VALUE_FILE_PATTERN_TO_PARSER_MAP);
    }

    private void testInvalidRawEventsConfig(String chainKey, String source, String expectedMessage) {
        TopicParserConfig topicParserConfig = new TopicParserConfig(chainKey, source, null, null);
        assertThatThrownBy(topicParserConfig::validate).isInstanceOf(IllegalArgumentException.class).hasMessage(expectedMessage);
    }

    private void testInvalidFilePatternMapConfig(HashMap<String, ParserChainSource> filePatternConfig, Class<?> expectedException, String expectedMessage) {
        TopicParserConfig topicParserConfig = new TopicParserConfig(null, null, null, filePatternConfig);
        assertThatThrownBy(topicParserConfig::validate).isInstanceOf(expectedException).hasMessage(expectedMessage);
    }
}
