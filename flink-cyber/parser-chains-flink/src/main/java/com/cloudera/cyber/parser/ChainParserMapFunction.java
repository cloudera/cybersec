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

import com.cloudera.cyber.Message;
import com.cloudera.cyber.parser.chain.resolver.ParserChainResolver;
import com.cloudera.cyber.parser.regex.resolver.CachedPatternResolver;
import com.cloudera.cyber.parser.chain.resolver.MessageFileParserChainResolver;
import com.cloudera.cyber.parser.chain.resolver.SingleMessageParserChainResolver;
import com.cloudera.cyber.parser.wrappers.AbstractParserOutput;
import com.cloudera.cyber.parser.wrappers.MessageFileParser;
import com.cloudera.cyber.parser.wrappers.SingleMessageParser;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Meter;
import org.apache.flink.metrics.MeterView;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

import java.security.PrivateKey;
import java.util.*;

import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class ChainParserMapFunction extends ProcessFunction<MessageToParse, Message> {

    public static final String CHAIN_PARSER_FEATURE = "chain_parser";
    public static final String TIMESTAMP_NOT_EPOCH = "Timestamp is not in epoch milliseconds or seconds. ";
    public static final String NO_TIMESTAMP_FIELD_MESSAGE = "Message does not contain a timestamp field.";

    @NonNull
    private final ParserChainMap chainConfig;
    @NonNull
    private final TopicPatternToChainMap topicMap;

    private final PrivateKey signKey;
    private final String allowedMessageFilePaths;

    private transient Meter messageMeter;
    private transient CachedPatternResolver<ParserChainResolver> topicToParserResolver;
    private final OutputTag<Message> errorOutputTag = new OutputTag<Message>(ParserJob.ERROR_MESSAGE_SIDE_OUTPUT){};
    private transient SingleMessageParser singleMessageParser;

    @EqualsAndHashCode(callSuper = true)
    @Data
    @AllArgsConstructor
    private static class ParserOutput extends AbstractParserOutput {
        private final OutputTag<Message> errorOutputTag;
        private final ProcessFunction<MessageToParse, Message>.Context context;
        private final Collector<Message> collector;

        @Override
        public void output(Message parsedMessage) {
            if (parsedMessage.getDataQualityMessages() != null) {
                context.output(errorOutputTag, parsedMessage);
            } else {
                collector.collect(parsedMessage);
            }
        }
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        log.info( "Chain config {}", chainConfig);
        log.info( "Topic map {}", topicMap);

        this.singleMessageParser = SingleMessageParser.create(chainConfig, signKey);

        MessageFileParser messageFileParser = createMessageFileParser();
        topicToParserResolver = new CachedPatternResolver<>(
                topicMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                        v -> getResolver(v.getValue(), messageFileParser, singleMessageParser))));

        messageMeter = getRuntimeContext().getMetricGroup().meter("messagesPerMinute", new MeterView(60));
    }

    private MessageFileParser createMessageFileParser() {
        if (allowedMessageFilePaths == null || allowedMessageFilePaths.isEmpty()) {
            if (topicMap.hasFileParser()) {
                throw new IllegalArgumentException("Can't initialize MessageFileParser because allowed paths is null or empty");
            }
            return null;
        } else {
            List<String> allowedMessageFilePathList = Arrays.asList(allowedMessageFilePaths.split(","));
            return MessageFileParser.create(allowedMessageFilePathList, singleMessageParser);
        }
    }

    private static ParserChainResolver getResolver(TopicParserConfig topicConfig, MessageFileParser messageFileParser, SingleMessageParser singleMessageParser) {
        if (topicConfig.getFilePatternToParserMap() != null) {
            return new MessageFileParserChainResolver(topicConfig, messageFileParser);
        } else {
            return new SingleMessageParserChainResolver(topicConfig, singleMessageParser);
        }
    }

    @Override
    public void processElement(MessageToParse message, Context context, Collector<Message> collector) {
        final String topic = message.getTopic();
        ParserChainResolver parserForTopic = topicToParserResolver.match(topic, t -> new SingleMessageParserChainResolver(new TopicParserConfig(topic, topic, null, null), singleMessageParser));
        parserForTopic.parse(message, new ParserOutput(errorOutputTag, context, collector));

        messageMeter.markEvent();
    }

}
