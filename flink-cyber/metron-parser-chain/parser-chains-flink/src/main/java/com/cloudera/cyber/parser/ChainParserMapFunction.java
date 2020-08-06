package com.cloudera.cyber.parser;

import com.cloudera.cyber.Message;
import com.cloudera.parserchains.core.*;
import com.cloudera.parserchains.core.catalog.ClassIndexParserCatalog;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;
import org.joda.time.Instant;

import java.util.*;
import java.util.stream.Collectors;

import static com.cloudera.parserchains.core.Constants.DEFAULT_INPUT_FIELD;

@RequiredArgsConstructor
@Slf4j
public class ChainParserMapFunction extends RichFlatMapFunction<MessageToParse, Message> {

    @NonNull
    private ParserChainMap chainConfig;
    @NonNull
    private Map<String, String> topicMap;

    private ChainRunner chainRunner;
    private Map<String, ChainLink> chains;

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        ChainBuilder chainBuilder = new DefaultChainBuilder(new ReflectiveParserBuilder(),
                new ClassIndexParserCatalog());
        chains = chainConfig.entrySet().stream().collect(Collectors.toMap(
                e -> e.getKey(),
                v ->
                {
                    try {
                        return chainBuilder.build(v.getValue());
                    } catch (InvalidParserException e) {
                        log.error("Cannot build parser chain", e);
                        return null;
                    }
                }));

        chainRunner = new DefaultChainRunner();
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
            log.warn("Timestamp missing from message on chain %s", message.getTopic());
            throw new IllegalStateException("Timestamp not present");
        }
        collector.collect(Message.newBuilder().setExtensions(fieldsFromChain(m.getFields()))
                .setTs(Instant.ofEpochMilli(Long.valueOf(timestamp.get().get())).toDateTime())
                .setOriginalSource(m.getField(FieldName.of(DEFAULT_INPUT_FIELD)).get().get())
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
