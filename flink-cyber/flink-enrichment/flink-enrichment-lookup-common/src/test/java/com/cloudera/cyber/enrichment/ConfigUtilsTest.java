package com.cloudera.cyber.enrichment;

import com.cloudera.cyber.enrichment.lookup.config.EnrichmentConfig;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentField;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentKind;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConfigUtilsTest {

    // test sources
    private static final String SQUID_SOURCE = "squid";
    private static final String NETFLOW_SOURCE = "netflow";

    // test enrichments
    private static final String MALICIOUS_IP_ENRICHMENT = "malicious_ip";
    private static final String MAJESTIC_MILLION_ENRICHMENT = "majestic_million";
    private static final String MALICIOUS_DOMAIN_ENRICHMENT = "malicious_domain";

    // test fields
    private static final String IP_DST_ADDR_FIELD = "ip_dst_addr";
    private static final String IP_SRC_ADDR_FIELD = "ip_src_addr";
    private static final String SECOND_LEVEL_DOMAIN_FIELD = "second_level_domain";

    // test configuration
    private static final List<EnrichmentField> hbaseFields = List.of(EnrichmentField.builder().enrichmentType(MALICIOUS_IP_ENRICHMENT).name(IP_SRC_ADDR_FIELD).build(),
            EnrichmentField.builder().enrichmentType(MALICIOUS_IP_ENRICHMENT).name(IP_DST_ADDR_FIELD).build(),
            EnrichmentField.builder().enrichmentType(MALICIOUS_DOMAIN_ENRICHMENT).name(SECOND_LEVEL_DOMAIN_FIELD).build());
    private static final EnrichmentConfig squidHbaseConfig = EnrichmentConfig.builder().source(SQUID_SOURCE).kind(EnrichmentKind.HBASE).fields(hbaseFields).build();
    private static final List<EnrichmentField> localFields = List.of(EnrichmentField.builder().enrichmentType(MAJESTIC_MILLION_ENRICHMENT).name(SECOND_LEVEL_DOMAIN_FIELD).build());
    private static final EnrichmentConfig squidLocalConfig = EnrichmentConfig.builder().source(SQUID_SOURCE).kind(EnrichmentKind.LOCAL).fields(localFields).build();
    private static final EnrichmentConfig netflowHbaseConfig = EnrichmentConfig.builder().source(NETFLOW_SOURCE).kind(EnrichmentKind.HBASE).fields(hbaseFields).build();
    private static final List<EnrichmentConfig> allConfigs = List.of(squidHbaseConfig, squidLocalConfig, netflowHbaseConfig);

    @Test
    public void getEnrichmentFieldPrefixesTest() {
        Map<String, Set<String>> expectedSourceToEnrichments = Map.of(SQUID_SOURCE, getExpectedEnrichmentPrefixes(Stream.of(squidHbaseConfig, squidLocalConfig)),
                                                                      NETFLOW_SOURCE, getExpectedEnrichmentPrefixes(Stream.of(netflowHbaseConfig)));
        assertEquals(expectedSourceToEnrichments, ConfigUtils.enrichmentFieldPrefixes(allConfigs));
        assertEquals(Collections.emptyMap(), ConfigUtils.enrichmentFieldPrefixes(Collections.emptyList()));
    }

    private Set<String> getExpectedEnrichmentPrefixes(Stream<EnrichmentConfig> configStream) {
        return configStream.flatMap(c -> c.getFields().stream()).
                map(f -> String.join(".", f.getName(), f.getEnrichmentType()).concat(".")).
                collect(Collectors.toSet());
    }
}
