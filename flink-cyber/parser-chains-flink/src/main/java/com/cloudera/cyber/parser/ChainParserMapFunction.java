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
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
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

    private byte[] EMPTY_SIGNATURE = new byte[1];

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
    private final OutputTag<Message> errorOutputTag = new OutputTag<Message>(ParserJob.PARSER_ERROR_SIDE_OUTPUT){};
    private transient Map<String, TopicParserConfig> topicNameToChain;
    private transient List<Tuple2<Pattern, TopicParserConfig>> topicPatternToChain;

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        log.info( "Chain config {}", chainConfig);
        log.info( "Topic map {}", topicMap);
        topicNameToChain = new HashMap<>();
        topicPatternToChain = topicMap.entrySet().stream().
                map( e -> new Tuple2<>(Pattern.compile(e.getKey()), e.getValue())).
                collect(Collectors.toList());
        ChainBuilder chainBuilder = new DefaultChainBuilder(new ReflectiveParserBuilder(),
                new ClassIndexParserCatalog());
        ArrayList<InvalidParserException> errors = new ArrayList<>();
        try {
            chains = chainConfig.entrySet().stream().collect(Collectors.toMap(
                    Map.Entry::getKey,
                    v ->
                    {
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
    public void processElement(MessageToParse message, Context context, Collector<Message> collector) throws Exception {
        final String inputMessage = message.getOriginalSource();
        final String topic = message.getTopic();
        final TopicParserConfig topicParserConfig = getChainForTopic(topic);
        final List<com.cloudera.parserchains.core.Message> run = chainRunner.run(inputMessage, chains.get(topicParserConfig.getChainKey()));
        final com.cloudera.parserchains.core.Message m = run.get(run.size() - 1);

        Optional<FieldValue> timestamp = m.getField(FieldName.of("timestamp"));

        long messageTimestamp = timestamp.map(t -> Long.valueOf(t.get())).
                orElseGet(() -> Instant.now().toEpochMilli());

        Optional<String> errorMessage = m.getError().map(Throwable::getMessage);
        if (!errorMessage.isPresent() && !timestamp.isPresent()) {
            errorMessage = Optional.of("Message does not contain a timestamp field.");
        }

        List<DataQualityMessage> dataQualityMessages = errorMessage.
                map(messageText -> Collections.singletonList(
                        DataQualityMessage.builder().
                                field(DEFAULT_INPUT_FIELD).
                                feature("chain_parser").
                                level(DataQualityMessageLevel.ERROR.name()).
                                message(messageText).
                                build())).
                orElse(null);

        String originalInput
                = m.getField(FieldName.of(DEFAULT_INPUT_FIELD)).get().get();
        byte[] sig = EMPTY_SIGNATURE;
        if (signature != null) {
            signature.update(originalInput.getBytes(StandardCharsets.UTF_8));
            sig = signature.sign();
        }
        Message parsedMessage = Message.builder().extensions(fieldsFromChain(errorMessage.isPresent(), m.getFields()))
                .source(topicParserConfig.getSource())
                .originalSource(SignedSourceKey.builder()
                        .topic(topic)
                        .partition(message.getPartition())
                        .offset(message.getOffset())
                        .signature(sig)
                        .build())
                .ts(messageTimestamp)
                .dataQualityMessages(dataQualityMessages)
                .build();

        if (dataQualityMessages != null) {
            context.output(errorOutputTag, parsedMessage);
        } else {
            collector.collect(parsedMessage);
        }
        messageMeter.markEvent();
    }

    private static Map<String, String> fieldsFromChain(boolean hasError, Map<FieldName, FieldValue> fields) {
        return fields.entrySet().stream().filter(mapEntry -> {
            String fieldName = mapEntry.getKey().get();
            if ((fieldName.equals(DEFAULT_INPUT_FIELD) && !hasError) || fieldName.equals("timestamp")) {
                return false;
            }
            return true;
        }).collect(Collectors.toMap(entryMap -> entryMap.getKey().get(), entryMap -> entryMap.getValue().get()));

    }

    private TopicParserConfig getChainForTopic(String topicName) {
        return topicNameToChain.computeIfAbsent(topicName, top ->
                topicPatternToChain.stream().filter(t -> t.f0.matcher(top).
                        matches()).findFirst().map(t -> t.f1).orElse(new TopicParserConfig(top, top, defaultKafkaBootstrap)));
    }

}
