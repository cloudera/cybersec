package com.cloudera.cyber.enrichment.hbase;

import com.cloudera.cyber.hbase.LookupKey;
import java.util.Collections;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.metron.enrichment.converter.EnrichmentConverter;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;
import org.apache.metron.enrichment.lookup.LookupKV;

@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Slf4j
@ToString(callSuper = true)
public class MetronLookupKey extends LookupKey {
    private final EnrichmentConverter converter = new EnrichmentConverter();
    private final String enrichmentType;

    @Override
    public Get toGet() {
        return converter.toGet(getCf(), new EnrichmentKey(enrichmentType, getKey()));
    }

    @Override
    public Map<String, Object> resultToMap(Result result) {
        try {
            LookupKV<EnrichmentKey, EnrichmentValue> keyValue = converter.fromResult(result, getCf());
            return keyValue.getValue().getMetadata();
        } catch (Exception e) {
            log.error(String.format("Unable to convert result Map: '%s'", result), e);
            return Collections.emptyMap();
        }
    }
}
