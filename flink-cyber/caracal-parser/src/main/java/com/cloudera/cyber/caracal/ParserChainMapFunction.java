package com.cloudera.cyber.caracal;

import com.cloudera.cyber.Message;
import com.cloudera.parserchains.core.*;
import com.cloudera.parserchains.core.catalog.ClassIndexParserCatalog;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Map function to apply the TMO parser chains to Messages
 */
@RequiredArgsConstructor
@Slf4j
public class ParserChainMapFunction extends RichMapFunction<Message, Message> {

    @NonNull
    private Map<String, SplitConfig> chainConfig;

    private ChainRunner chainRunner;
    private Map<String, ChainLink> chains;

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        ChainBuilder chainBuilder = new DefaultChainBuilder(new ReflectiveParserBuilder(),
                new ClassIndexParserCatalog());
        chains = chainConfig.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v ->
        {
            try {
                return chainBuilder.build(v.getValue().getChainSchema());
            } catch (InvalidParserException e) {
                log.error("Cannot build parser chain", e);
                return null;
            }
        }));

        chainRunner = new DefaultChainRunner();
    }

    @Override
    public Message map(Message message) {
        String source = message.getSource();
        if (!chains.containsKey(source)) {
            log.warn(String.format("No parser chain found for topic %s", source));
            return message;
        }
        List<com.cloudera.parserchains.core.Message> results = new ArrayList<>();

        com.cloudera.parserchains.core.Message.Builder builder = com.cloudera.parserchains.core.Message.builder();
        message.getExtensions().entrySet().forEach(e -> {
            builder
                .addField(e.getKey(), e.getValue().toString()).build();
            });

        List<com.cloudera.parserchains.core.Message> out = chainRunner.run(builder.build(), chains.get(source), results);
        com.cloudera.parserchains.core.Message lastMessage = out.get(out.size() - 1);

        return message.toBuilder().extensions(lastMessage.getFields().entrySet().stream()
                .collect(Collectors.toMap(k -> k.getKey().get(), v->v.getValue().get())))
                .build();
    }
}
