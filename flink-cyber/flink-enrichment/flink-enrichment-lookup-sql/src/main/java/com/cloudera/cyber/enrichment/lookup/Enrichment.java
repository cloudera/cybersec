package com.cloudera.cyber.enrichment.lookup;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.flink.api.common.typeinfo.TypeInfo;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TypeInfo(EnrichmentTypeFactory.class)
public class Enrichment {
    private long ts;
    private String pri;
    private Map<String, String> entries;
}
