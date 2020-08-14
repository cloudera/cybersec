package com.cloudera.cyber.enrichment.lookup;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.util.Collector;

import java.util.List;

@Slf4j
public class ExtractEnrichables extends RichFlatMapFunction<Message, Enrichable> {
    private final List<EnrichmentConfig> config;

    public ExtractEnrichables(List<EnrichmentConfig> config) {
        super();
        this.config = config;
    }

    @Override
    public void flatMap(final Message message, Collector<Enrichable> collector) throws Exception {
        config.stream()
                .filter(f -> f.getSource().equals(message.getSource()))
                .flatMap(c -> {
                    System.out.println((c.getFields()));
                    return c.getFields().stream().map(f -> Tuple2.of(f.getName(), f.getEnrichmentType()));
                })
                .filter(c -> message.getExtensions().containsKey(c.f0))
                .map(f -> {
                    String v = message.getExtensions().get(f.f0).toString();
                    return Enrichable.builder()
                            .ts(message.getTs())
                            .id(message.getId())
                            .lookupKey(f.f1 + ":" + v)
                            .field(f.f0)
                            .enrichmentType(f.f1)
                            .fieldValue(v)
                            .build();
                }).forEach(e -> collector.collect(e));
    }

}
