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

import static com.cloudera.parserchains.core.Constants.DEFAULT_INPUT_FIELD;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.DataQualityMessageLevel;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.SignedSourceKey;
import com.cloudera.parserchains.core.ChainBuilder;
import com.cloudera.parserchains.core.ChainLink;
import com.cloudera.parserchains.core.ChainRunner;
import com.cloudera.parserchains.core.DefaultChainBuilder;
import com.cloudera.parserchains.core.DefaultChainRunner;
import com.cloudera.parserchains.core.FieldName;
import com.cloudera.parserchains.core.FieldValue;
import com.cloudera.parserchains.core.InvalidParserException;
import com.cloudera.parserchains.core.ReflectiveParserBuilder;
import com.cloudera.parserchains.core.catalog.ClassIndexParserCatalog;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Meter;
import org.apache.flink.metrics.MeterView;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

@RequiredArgsConstructor
@Slf4j
public class ChainParserMapFunction extends ProcessFunction<MessageToParse, Message> {

    public static final String CHAIN_PARSER_FEATURE = "chain_parser";
    public static final String TIMESTAMP_NOT_EPOCH = "Timestamp is not in epoch milliseconds or seconds. ";
    public static final String NO_TIMESTAMP_FIELD_MESSAGE = "Message does not contain a timestamp field.";
    public static final byte[] EMPTY_SIGNATURE = new byte[1];

    @NonNull
    private final ParserChainMap chainConfig;
    @NonNull
    private final TopicPatternToChainMap topicMap;

    private final PrivateKey signKey;
    private final String defaultKafkaBootstrap;

    private transient ChainRunner chainRunner;
    private transient Map<String, ChainLink> chains;
    private transient Signature signature;
    private transient Meter messageMeter;
    private final OutputTag<Message> errorOutputTag = new OutputTag<Message>(ParserJob.ERROR_MESSAGE_SIDE_OUTPUT) {
    };
    private transient Map<String, TopicParserConfig> topicNameToChain;
    private transient List<Tuple2<Pattern, TopicParserConfig>> topicPatternToChain;

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        log.info("Chain config {}", chainConfig);
        log.info("Topic map {}", topicMap);
        topicNameToChain = new HashMap<>();
        topicPatternToChain = topicMap.entrySet().stream()
                                      .map(e -> new Tuple2<>(Pattern.compile(e.getKey()), e.getValue()))
                                      .collect(Collectors.toList());
        ChainBuilder chainBuilder = new DefaultChainBuilder(new ReflectiveParserBuilder(),
              new ClassIndexParserCatalog());
        ArrayList<InvalidParserException> errors = new ArrayList<>();
        try {
            chains = chainConfig.entrySet().stream().collect(Collectors.toMap(
                  Map.Entry::getKey,
                  v -> {
                      try {
                          return chainBuilder.build(v.getValue());
                      } catch (InvalidParserException e) {
                          log.error("Cannot build parser chain", e);
                          errors.add(e);
                          return null;
                      }
                  }));
        } catch (NullPointerException e) {
            if (CollectionUtils.isNotEmpty(errors)) {
                throw errors.get(0);
            }
        }

        chainRunner = new DefaultChainRunner();

        if (signKey != null) {
            signature = Signature.getInstance("SHA1WithRSA");
            signature.initSign(signKey);
        } else {
            signature = null;
        }
        messageMeter = getRuntimeContext().getMetricGroup().meter("messagesPerMinute", new MeterView(60));
        log.info("Parser chains {}", chains);
        log.info("Chain runner chains {}", chainRunner);
    }

    @Override
    public void processElement(MessageToParse message, Context context, Collector<Message> collector) {
        final String topic = message.getTopic();
        final TopicParserConfig topicParserConfig = getChainForTopic(topic);
        final ChainLink chain = chains.get(topicParserConfig.getChainKey());

        final List<com.cloudera.parserchains.core.Message> run = chainRunner.run(message, chain);
        final com.cloudera.parserchains.core.Message m = run.get(run.size() - 1);
        if (m.getEmit()) {
            Optional<String> errorMessage = m.getError().map(Throwable::getMessage);
            long messageTimestamp = Instant.now().toEpochMilli();

            if (!errorMessage.isPresent()) {
                Optional<FieldValue> timestamp = m.getField(FieldName.of("timestamp"));

                if (timestamp.isPresent()) {
                    try {
                        // handle timestamps that are <seconds>.<milliseconds>
                        String timestampString = timestamp.get().get().replace(".", "");
                        messageTimestamp = Long.parseLong(timestampString);
                        if (timestampString.length() < 12) {
                            // normalize second times
                            messageTimestamp *= 1000;
                        }
                    } catch (NumberFormatException nfe) {
                        errorMessage = Optional.of(TIMESTAMP_NOT_EPOCH.concat(nfe.getMessage()));
                    }
                } else {
                    errorMessage = Optional.of(NO_TIMESTAMP_FIELD_MESSAGE);
                }
            }

            List<DataQualityMessage> dataQualityMessages = errorMessage
                  .map(messageText -> Collections.singletonList(
                        DataQualityMessage.builder()
                                          .field(DEFAULT_INPUT_FIELD)
                                          .feature(CHAIN_PARSER_FEATURE)
                                          .level(DataQualityMessageLevel.ERROR.name())
                                          .message(messageText)
                                          .build()))
                  .orElse(null);

            Message parsedMessage =
                  Message.builder().extensions(fieldsFromChain(errorMessage.isPresent(), m.getFields()))
                         .source(topicParserConfig.getSource())
                         .originalSource(SignedSourceKey.builder()
                                                        .topic(topic)
                                                        .partition(message.getPartition())
                                                        .offset(message.getOffset())
                                                        .signature(signOriginalText(m))
                                                        .build())
                         .ts(messageTimestamp)
                         .dataQualityMessages(dataQualityMessages)
                         .build();

            if (dataQualityMessages != null) {
                context.output(errorOutputTag, parsedMessage);
            } else {
                collector.collect(parsedMessage);
            }
        }
        messageMeter.markEvent();
    }

    private byte[] signOriginalText(com.cloudera.parserchains.core.Message m) {

        if (signature != null) {
            Optional<FieldValue> originalMessage = m.getField(FieldName.of(DEFAULT_INPUT_FIELD));
            if (originalMessage.isPresent()) {
                byte[] bytes = originalMessage.get().toBytes();
                try {
                    signature.update(bytes);
                    return signature.sign();
                } catch (SignatureException e) {
                    // this should not happen because signature is intialized but log it just in case
                    log.error("Failed to sign message.", e);
                }
            }
        }

        return EMPTY_SIGNATURE;
    }

    private static Map<String, String> fieldsFromChain(boolean hasError, Map<FieldName, FieldValue> fields) {
        return fields.entrySet().stream().filter(mapEntry -> {
            String fieldName = mapEntry.getKey().get();
            return (!StringUtils.equals(fieldName, DEFAULT_INPUT_FIELD) || hasError) && !fieldName.equals("timestamp");
        }).collect(Collectors.toMap(entryMap -> entryMap.getKey().get(), entryMap -> entryMap.getValue().get()));

    }

    private TopicParserConfig getChainForTopic(String topicName) {
        return topicNameToChain.computeIfAbsent(topicName, top ->
              topicPatternToChain.stream().filter(t -> t.f0.matcher(top)
                                                           .matches()).findFirst().map(t -> t.f1)
                                 .orElse(new TopicParserConfig(top, top, defaultKafkaBootstrap)));
    }

}
