package com.cloudera.cyber.indexing;

import com.cloudera.cyber.Message;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.functions.co.KeyedCoProcessFunction;
import org.apache.flink.util.Collector;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public class FilterStreamFieldsByConfig extends KeyedCoProcessFunction<String, Message, Map.Entry<String, Set<String>>, IndexEntry> {
    private MapState<String, Set<String>> fieldsRequiredMap = null;

    @Override
    public void processElement1(Message message, Context context, Collector<IndexEntry> collector) throws Exception {
        Map<String, Set<String>> fieldsRequiredMap = null;
        Set<String> fieldsRequired = fieldsRequiredMap.get(message.getSource());

        Stream<Tuple2<String, String>> messageFields = message.getExtensions().entrySet().stream()
                .filter(f -> fieldsRequired.contains(f.getKey()))
                .map(e -> Tuple2.of(e.getKey(), e.getValue().toString()));

        Stream<Tuple2<String, String>> threatFields = message.getThreats().entrySet().stream().flatMap(e ->
                e.getValue().stream().flatMap(l ->
                        l.getFields().entrySet().stream().map(le -> Tuple2.of(
                                String.join(".", new String[]{e.getKey(), l.getObservableType(), l.getObservable(), e.getKey()}),
                                le.getValue())
                        )
                )
        );
        Stream<Tuple2<String, String>> baseFields = Stream.of(
                Tuple2.of("message", message.getMessage()),
                Tuple2.of("timestamp", message.getTs().toString())
        );

        Stream<Tuple2<String, String>> allFields = Stream.of(
                baseFields,
                messageFields,
                threatFields
        ).flatMap(s->s);

        Map<String, String> fields = allFields.collect(toMap(e -> e.f0, e -> e.f1));

        collector.collect(IndexEntry.builder()
                .index(message.getSource())
                .id(message.getId())
                .timestamp(message.getTs())
                .fields(fields)
                .build());
    }

    @Override
    public void processElement2(Map.Entry<String, Set<String>> stringListEntry, Context context, Collector<IndexEntry> collector) throws Exception {
        fieldsRequiredMap.put(stringListEntry.getKey(), stringListEntry.getValue());
    }
}
