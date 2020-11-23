package com.cloudera.cyber.indexing;

import com.cloudera.cyber.Message;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction;
import org.apache.flink.util.Collector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

@Slf4j
public class FilterStreamFieldsByConfig extends KeyedBroadcastProcessFunction<String, Message, CollectionField, IndexEntry> {

    private final MapStateDescriptor<String, List<String>> broadcastState;

    public FilterStreamFieldsByConfig(MapStateDescriptor<String, List<String>> broadcastState) {
        this.broadcastState = broadcastState;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);

    }

    @Override
    public void processElement(Message message, ReadOnlyContext readOnlyContext, Collector<IndexEntry> collector) throws Exception {
        if (!readOnlyContext.getBroadcastState(broadcastState).contains(message.getSource())) {
            return;
        }

        List<String> fieldsRequired = readOnlyContext.getBroadcastState(broadcastState).get(message.getSource());

        Stream<Tuple2<String, String>> messageFields = message.getExtensions() == null ? Stream.empty() :
                message.getExtensions().entrySet().stream()
                        .filter(f -> !f.getKey().equals("ts") && fieldsRequired.contains(f.getKey()))
                        .map(e -> Tuple2.of(e.getKey(), e.getValue()));

        Stream<Tuple2<String, String>> threatFields = message.getThreats() == null ? Stream.empty() :
                message.getThreats().entrySet().stream().flatMap(e ->
                        e.getValue().stream().flatMap(l ->
                                l.getFields().entrySet().stream().map(le -> Tuple2.of(
                                        String.join(".", new String[]{e.getKey(), l.getObservableType(), l.getObservable(), e.getKey()}),
                                        le.getValue())
                                )
                        )
                );
        Stream<Tuple2<String, String>> baseFields = Stream.of(
                Tuple2.of("message", message.getMessage())
        );

        Stream<Tuple2<String, String>> allFields = Stream.of(
                baseFields,
                messageFields,
                threatFields)
                .flatMap(s->s)
                .filter(r -> r != null && r.f1 != null);

        Map<String, String> fields = allFields.collect(toMap(
                e -> e.f0,
                e -> e.f1)
        );

        collector.collect(IndexEntry.builder()
                .index(message.getSource())
                .id(message.getId())
                .timestamp(message.getTs())
                .fields(fields)
                .build());
    }

    @Override
    public void processBroadcastElement(CollectionField collectionFields, Context context, Collector<IndexEntry> collector) throws Exception {
        log.info("Schema Update message {}", collectionFields.toString());
        context.getBroadcastState(broadcastState).put(collectionFields.getKey(), new ArrayList<>(collectionFields.getValues()));
    }
}
