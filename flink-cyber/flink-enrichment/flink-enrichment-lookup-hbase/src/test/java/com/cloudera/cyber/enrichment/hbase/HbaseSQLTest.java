package com.cloudera.cyber.enrichment.hbase;

import com.cloudera.cyber.enrichment.lookup.config.EnrichmentConfig;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentField;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentKind;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@Ignore
public class HbaseSQLTest {

    @Test
    public void testSingleEnrichment() {
        List<EnrichmentConfig> configs = Arrays.asList(
                EnrichmentConfig.builder()
                        .kind(EnrichmentKind.HBASE)
                        .source("test")
                        .fields(Arrays.asList(
                                EnrichmentField.builder()
                                        .name("a")
                                        .enrichmentType("enrichmentA")
                                        .build()
                        ))
                        .build()
        );
        String sql = HbaseSQL.buildSql(configs);

        assertForAll(sql);
        assertThat(sql, containsString("MAP_PREFIX(HBASE_ENRICHMENT(messages.ts, 'enrichmentA', messages.extensions['a']), 'a.enrichmentA')"));
    }

    @Test
    public void testTwoEnrichmentsOnOneField() {
        List<EnrichmentConfig> configs = Arrays.asList(
                EnrichmentConfig.builder()
                        .kind(EnrichmentKind.HBASE)
                        .source("test")
                        .fields(Arrays.asList(
                                EnrichmentField.builder()
                                        .name("a")
                                        .enrichmentType("enrichmentA")
                                        .build(),
                                EnrichmentField.builder()
                                        .name("a")
                                        .enrichmentType("enrichmentB")
                                        .build()
                        ))
                        .build()
        );
        String sql = HbaseSQL.buildSql(configs);

        assertForAll(sql);
        assertThat(sql, containsString("MAP_PREFIX(HBASE_ENRICHMENT(messages.ts, 'enrichmentA', messages.extensions['a']), 'a.enrichmentA')"));
        assertThat(sql, containsString("MAP_PREFIX(HBASE_ENRICHMENT(messages.ts, 'enrichmentB', messages.extensions['a']), 'a.enrichmentB')"));
    }

    @Test
    public void testTwoEnrichmentsOnTwoFields() {
        List<EnrichmentConfig> configs = Arrays.asList(
                EnrichmentConfig.builder()
                        .kind(EnrichmentKind.HBASE)
                        .source("test")
                        .fields(Arrays.asList(
                                EnrichmentField.builder()
                                        .name("a")
                                        .enrichmentType("enrichmentA")
                                        .build(),
                                EnrichmentField.builder()
                                        .name("b")
                                        .enrichmentType("enrichmentA")
                                        .build()
                        ))
                        .build()
        );
        String sql = HbaseSQL.buildSql(configs);

        assertForAll(sql);
        assertThat(sql, containsString("MAP_PREFIX(HBASE_ENRICHMENT(messages.ts, 'enrichmentA', messages.extensions['a']), 'a.enrichmentA')"));
        assertThat(sql, containsString("MAP_PREFIX(HBASE_ENRICHMENT(messages.ts, 'enrichmentA', messages.extensions['b']), 'b.enrichmentA')"));
    }

    private void assertForAll(String sql) {
        assertThat(sql, notNullValue());
        assertThat(sql, not(containsStringIgnoringCase("left join")));
        assertThat(sql, not(containsStringIgnoringCase("left outer join")));
    }
}
