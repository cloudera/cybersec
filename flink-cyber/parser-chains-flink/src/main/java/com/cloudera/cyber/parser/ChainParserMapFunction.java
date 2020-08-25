package com.cloudera.cyber.parser;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.SignedSourceKey;
import com.cloudera.cyber.sha1;
import com.cloudera.parserchains.core.*;
import com.cloudera.parserchains.core.catalog.ClassIndexParserCatalog;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.*;
import java.util.stream.Collectors;

import static com.cloudera.parserchains.core.Constants.DEFAULT_INPUT_FIELD;

@RequiredArgsConstructor
@Slf4j
public class ChainParserMapFunction extends RichFlatMapFunction<MessageToParse, Message> {

    @NonNull
    private final ParserChainMap chainConfig;
    @NonNull
    private final Map<String, String> topicMap;

    @NonNull
    private final PrivateKey signKey;

    private transient ChainRunner chainRunner;
    private transient Map<String, ChainLink> chains;
    private transient Signature signature;

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
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
    }

    /**
     * TODO - Handle errors
     *
     * @param message
     * @param collector
     * @throws Exception
     */
    @Override
    public void flatMap(MessageToParse message, Collector<Message> collector) throws Exception {
        final String inputMessage = message.getOriginalSource();
        final String chainKey = topicMap.getOrDefault(message.getTopic(), message.getTopic());

        final List<com.cloudera.parserchains.core.Message> run = chainRunner.run(inputMessage, chains.get(chainKey));
        final com.cloudera.parserchains.core.Message m = run.get(run.size() - 1);

        Optional<FieldValue> timestamp = m.getField(FieldName.of("timestamp"));

        if (!timestamp.isPresent()) {
            log.warn(String.format("Timestamp missing from message on chain %s", message.getTopic()));
            throw new IllegalStateException("Timestamp not present");
        }
        String originalInput
                = m.getField(FieldName.of(DEFAULT_INPUT_FIELD)).get().get();

        signature.update(originalInput.getBytes(StandardCharsets.UTF_8));
        byte[] sig = signature.sign();

        collector.collect(Message.newBuilder().setExtensions(fieldsFromChain(m.getFields()))
                .setOriginalSource(SignedSourceKey.newBuilder()
                        .setTopic(message.getTopic())
                        .setPartition(message.getPartition())
                        .setOffset(message.getOffset())
                        .setSignature(new sha1(sig))
                        .build())
                .setTs(Long.valueOf(timestamp.get().get()))
                .setId(UUID.randomUUID().toString())
                .build());
    }

    private static HashMap<String, Object> fieldsFromChain(Map<FieldName, FieldValue> fields) {
        return new HashMap<String, Object>() {{
            fields.entrySet().stream().forEach(e -> {
                String key = e.getKey().get();
                if (key.equals(DEFAULT_INPUT_FIELD) || key.equals("timestamp")) {
                } else {
                    put(key, e.getValue().get());
                }
            });
        }};
    }

}