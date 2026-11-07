package com.cloudera.cyber.ondemand.source.decode;

import com.cloudera.cyber.ondemand.config.OnDemandEnrichmentConfigurationHelper;
import com.cloudera.cyber.ondemand.config.OndemandEnrichmentConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class ReadTableSqlTest {

    @TempDir
    Path tempDir;

    @Test
    public void testSqlGeneration() throws Exception {
        OnDemandEnrichmentConfigurationHelper helper = new OnDemandEnrichmentConfigurationHelper();
        OndemandEnrichmentConfiguration config = helper.readConfiguration(tempDir, "squid", "destdb");
        List<String> actualSql = ReadTableSql.generateSelectStatements(config, "iceberg_catalog", "cyber");
        List<String> expectedSqls = Collections.singletonList("select id, domain, ts, fields, source from `iceberg_catalog`.`cyber`.`examples_full_events_raw` where (dt > '2026-09-01' OR (dt = '2026-09-01' AND hr >= '17')) and (dt < '2026-09-02' OR (dt = '2026-09-02' AND hr <= '18')) and ts >= TIMESTAMP '2026-09-01 17:15:00.035' and ts <= TIMESTAMP '2026-09-02 18:30:00.000' and source in ( 'squid' ) ");
        Assertions.assertEquals(expectedSqls, actualSql);
    }
}
