package com.cloudera.cyber.parser;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.DataQualityMessageLevel;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.SignedSourceKey;
import com.cloudera.parserchains.core.*;
import com.cloudera.parserchains.core.catalog.ClassIndexParserCatalog;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.cloudera.parserchains.core.Constants.DEFAULT_INPUT_FIELD;

@RequiredArgsConstructor
@Slf4j
public class ChainParserMapFunction extends ProcessFunction<MessageToParse, Message> {

    @NonNull
    private final ParserChainMap chainConfig;
    @NonNull
    private final Map<String, String> topicMap;

    @NonNull
    private final PrivateKey signKey;

    private transient ChainRunner chainRunner;
    private transient Map<String, ChainLink> chains;
    private transient Signature signature;
    private final OutputTag<Message> errorOutputTag = new OutputTag<Message>(ParserJob.PARSER_ERROR_SIDE_OUTPUT){};


    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        log.info( "Chain config {}", chainConfig);
        log.info( "Topic map {}", topicMap);
        ChainBuilder chainBuilder = new DefaultChainBuilder(new ReflectiveParserBuilder(),
                new ClassIndexParserCatalog());
        ArrayList<InvalidParserException> errors = new ArrayList<>();
        try {
            chains = chainConfig.entrySet().stream().collect(Collectors.toMap(
                    e -> e.getKey(),
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
            if (errors.size() > 0) {
                throw errors.get(0);
            }
        }

        chainRunner = new DefaultChainRunner();

        signature = Signature.getInstance("SHA1WithRSA");
        signature.initSign(signKey);
        log.info("Parser chains {}", chains);
        log.info("Chain runner chains {}", chainRunner);
    }

    @Override
    public void processElement(MessageToParse message, Context context, Collector<Message> collector) throws Exception {
        final String inputMessage = message.getOriginalSource();
        final String chainKey = topicMap.getOrDefault(message.getTopic(), message.getTopic());

        final List<com.cloudera.parserchains.core.Message> run = chainRunner.run(inputMessage, chains.get(chainKey));
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

        signature.update(originalInput.getBytes(StandardCharsets.UTF_8));
        byte[] sig = signature.sign();
        Message parsedMessage = Message.builder().extensions(fieldsFromChain(errorMessage.isPresent(), m.getFields()))
                .source(message.getTopic())
                .originalSource(SignedSourceKey.builder()
                        .topic(message.getTopic())
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
    }

    private static HashMap<String, String> fieldsFromChain(boolean hasError, Map<FieldName, FieldValue> fields) {
        return new HashMap<String, String>() {{
            fields.entrySet().stream().forEach(e -> {
                String key = e.getKey().get();
                if ((key.equals(DEFAULT_INPUT_FIELD) && !hasError) || key.equals("timestamp")) {
                } else {
                    put(key, e.getValue().get());
                }
            });
        }};
    }

}
