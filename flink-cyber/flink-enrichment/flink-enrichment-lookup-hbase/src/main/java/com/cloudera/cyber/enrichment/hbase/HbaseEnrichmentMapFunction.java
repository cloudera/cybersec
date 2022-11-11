package com.cloudera.cyber.enrichment.hbase;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageConfig;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentsConfig;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentConfig;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentField;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentKind;
import com.cloudera.cyber.hbase.AbstractHbaseMapFunction;
import com.cloudera.cyber.hbase.LookupKey;
import com.google.common.base.Joiner;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Slf4j
@Getter
public class HbaseEnrichmentMapFunction extends AbstractHbaseMapFunction<Message, Message> {
    private final Map<String, List<EnrichmentField>> fieldToLookup;
    private final Set<String> sources;
    private final EnrichmentsConfig enrichmentStorageConfig;

    public HbaseEnrichmentMapFunction(List<EnrichmentConfig> configs, com.cloudera.cyber.enrichment.hbase.config.EnrichmentsConfig enrichmentStorageConfig) {
        super();

        sources = configs.stream()
                .filter(c -> c.getKind().equals(EnrichmentKind.HBASE))
                .map(EnrichmentConfig::getSource)
                .collect(Collectors.toSet());

        fieldToLookup = configs.stream()
                .filter(c -> c.getKind().equals(EnrichmentKind.HBASE))
                .collect(toMap(
                        EnrichmentConfig::getSource, EnrichmentConfig::getFields)
                );

        log.info("Applying HBase enrichments to the following sources: {}", sources);

        this.enrichmentStorageConfig = enrichmentStorageConfig;
    }

    private LookupKey buildLookup(String enrichmentType, String fieldValue) {
        EnrichmentStorageConfig storageConfig = enrichmentStorageConfig.getStorageForEnrichmentType(enrichmentType);
        return storageConfig.getFormat().getLookupBuilder().build(storageConfig, enrichmentType, fieldValue);
    }

    @Override
    public Message map(Message message) {
        if (sources.stream().noneMatch(s -> message.getSource().equals(s))) return message;

        messageCounter.inc(1);

        return MessageUtils.addFields(message, fieldToLookup.get(message.getSource()).stream().flatMap(
                field -> {
                    String enrichmentType = field.getEnrichmentType();
                    String key = message.getExtensions().get(field.getName());

                    return ((key == null) ? notFound() : hbaseLookup(message.getTs(),
                            buildLookup(enrichmentType, key),
                            Joiner.on(".").join(field.getName(), field.getEnrichmentType())
                    )).entrySet().stream();
                })
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b)));
    }

}

