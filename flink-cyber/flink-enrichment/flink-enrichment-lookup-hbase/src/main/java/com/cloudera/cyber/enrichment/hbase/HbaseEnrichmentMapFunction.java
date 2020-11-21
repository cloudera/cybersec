package com.cloudera.cyber.enrichment.hbase;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentConfig;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentField;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentKind;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Slf4j
@Getter
public class HbaseEnrichmentMapFunction extends AbstractHbaseMapFunction {
    private final Map<String, List<EnrichmentField>> fieldToLookup;
    private final Set<String> sources;
    private final String tableName;

    public HbaseEnrichmentMapFunction(List<EnrichmentConfig> configs, String tableName) throws IOException {
        super();

        sources = configs.stream()
                .filter(c -> c.getKind().equals(EnrichmentKind.HBASE))
                .map(c -> c.getSource())
                .collect(Collectors.toSet());

        fieldToLookup = configs.stream()
                .filter(c -> c.getKind().equals(EnrichmentKind.HBASE))
                .collect(toMap(
                        k -> k.getSource(), v -> v.getFields())
                );

        log.info("Applying HBase enrichments to the following sources: %s", sources);

        this.tableName = tableName;
    }

    @Override
    public Message map(Message message) {
        if (sources.stream().noneMatch(s -> message.getSource().equals(s))) return message;

        messageCounter.inc(1);

        return MessageUtils.addFields(message, fieldToLookup.get(message.getSource()).stream().map(
                field -> {
                    byte[] cf = Bytes.toBytes(field.getEnrichmentType());
                    String key = message.getExtensions().get(field.getName());
                    return ((key == null) ? notFound() : hbaseLookup(message.getTs(),
                            LookupKey.builder()
                                    .key(Bytes.toBytes(key))
                                    .cf(cf)
                                    .build(),
                            field.getName())).entrySet().stream();
                }).flatMap(l -> l)
                .collect(toMap(k -> k.getKey(), v -> v.getValue(), (a, b) -> b)));
    }
}

