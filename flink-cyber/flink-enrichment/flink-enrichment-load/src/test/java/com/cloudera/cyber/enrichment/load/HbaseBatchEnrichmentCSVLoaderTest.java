package com.cloudera.cyber.enrichment.load;

import com.cloudera.cyber.EnrichmentEntry;
import com.cloudera.cyber.commands.CommandType;
import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentsConfig;
import com.google.common.collect.ImmutableMap;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.CollectingSink;
import org.apache.flink.test.util.JobTester;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HbaseBatchEnrichmentCSVLoaderTest extends BatchEnrichmentLoaderCSV {


    private final CollectingSink<EnrichmentCommand> sink = new CollectingSink<>();

    private static final String MAJESTIC_MILLION_CF = "majestic_million";
    private static final Map<String, String> firstExtensions = ImmutableMap.<String,String>builder().
            put("GlobalRank", "1").
            put("TldRank", "1").
            put("TLD", "com").
            put("RefSubNets", "494155").
            put("RefIPs", "2890130").
            put("IDN_Domain", "facebook.com").
            put("IDN_TLD", "com").
            put("PrevGlobalRank", "1").
            put("PrevTldRank", "1").
            put("PrevRefSubNets", "493314").
            put("PrevRefIPs", "2868293").
            build();

    private static final Map<String, String> lastExtensions = ImmutableMap.<String,String>builder().
            put("GlobalRank", "9").
            put("TldRank", "1").
            put("TLD", "org").
            put("RefSubNets", "285566").
            put("RefIPs", "1126132").
            put("IDN_Domain", "wikipedia.org").
            put("IDN_TLD", "org").
            put("PrevGlobalRank", "9").
            put("PrevTldRank", "1").
            put("PrevRefSubNets", "284262").
            put("PrevRefIPs", "1111606").
            build();

    private static final Map<String, Map<String, String>>  extensionsWithAllData =
            ImmutableMap.of("facebook.com", firstExtensions,
                    "wikipedia.org", lastExtensions);

    @Test
    public void testCSVLoad() throws Exception {
        String columns = "GlobalRank,TldRank,Domain,TLD,RefSubNets,RefIPs,IDN_Domain,IDN_TLD,PrevGlobalRank,PrevTldRank,PrevRefSubNets,PrevRefIPs";
        testLoadStream(columns, extensionsWithAllData);
        testLoadFromEnrichmentStorageConfig("all_fields_enrichment_config.json", columns, extensionsWithAllData);
    }

    @Test
    public void testCSVIgnoreColumns() throws Exception {
        Map<String, Map<String, String>> expectedReducedValues = extensionsWithAllData.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> ImmutableMap.of("GlobalRank", v.getValue().get("GlobalRank"))));
        String columns = "GlobalRank,,Domain,,,,,,,,,";
        testLoadStream(columns, expectedReducedValues);
        testLoadFromEnrichmentStorageConfig("specific_fields_enrichment_config.json", columns, expectedReducedValues);
    }

    public void testLoadFromEnrichmentStorageConfig(String testFileName, String enrichmentFields, Map<String, Map<String, String>> enrichmentsToVerify) throws Exception {
        ParameterTool params = ParameterTool.fromMap(ImmutableMap.<String, String>builder().
                put(ENRICHMENT_SOURCE_FILE, "./src/test/resources/majestic_million_sample.csv").
                put(ENRICHMENT_SKIP_FIRST_LINE, "true").
                put(ENRICHMENT_COLUMNS, enrichmentFields).
                put(PARAMS_ENRICHMENT_FILE, getJsonPath(testFileName)).
                put(ENRICHMENT_TYPE, MAJESTIC_MILLION_CF).
                put(PARAMS_ENRICHMENTS_TABLE, "enrichments").
                build());
        testLoad(params, enrichmentsToVerify);
    }

    private void testLoadStream(String enrichmentFields, Map<String, Map<String, String>> enrichmentsToVerify) throws Exception {
        ParameterTool params = ParameterTool.fromMap(ImmutableMap.<String, String>builder().
                put(ENRICHMENT_SOURCE_FILE, "./src/test/resources/majestic_million_sample.csv").
                put(ENRICHMENT_SKIP_FIRST_LINE, "true").
                put(ENRICHMENT_COLUMNS, enrichmentFields).
                put(ENRICHMENT_KEY_FIELD_NAME, "Domain").
                put(ENRICHMENT_TYPE, MAJESTIC_MILLION_CF).
                put(PARAMS_ENRICHMENTS_TABLE, "enrichments").
                build());
        testLoad(params, enrichmentsToVerify);
    }

    private void testLoad(ParameterTool params, Map<String, Map<String, String>> enrichmentsToVerify) throws Exception {
        JobTester.startTest(runPipeline(params));

        List<EnrichmentCommand> enrichmentCommands = new ArrayList<>();

        for(int i = 0; i < 9; i++) {
            enrichmentCommands.add(sink.poll());
        }
        JobTester.stopTest();
        verifyEnrichmentCommands(enrichmentsToVerify, enrichmentCommands);
    }

    private void verifyEnrichmentCommands(Map<String, Map<String, String>> enrichmentsToVerify, List<EnrichmentCommand> enrichmentCommands) {
        for(EnrichmentCommand command: enrichmentCommands) {
            Assert.assertEquals(CommandType.ADD, command.getType());
            Assert.assertEquals(Collections.emptyMap(), command.getHeaders());
            EnrichmentEntry entry = command.getPayload();
            Assert.assertFalse(entry.getKey().isEmpty());
            Assert.assertEquals(MAJESTIC_MILLION_CF, entry.getType());
            Instant earliestTime = Instant.now().minus(5, ChronoUnit.MINUTES);
            Instant timestampInstant = Instant.ofEpochMilli(entry.getTs());
            Assert.assertTrue(earliestTime.isBefore(timestampInstant));
            Assert.assertTrue(timestampInstant.isBefore(Instant.now()));
            Map<String, String> expectedEnrichmentValues = enrichmentsToVerify.get(entry.getKey());
            if (expectedEnrichmentValues != null) {
                Assert.assertEquals(expectedEnrichmentValues, entry.getEntries());
            }
        }
        Assert.assertEquals(9, enrichmentCommands.size());
    }

    @Override
    protected void writeResults(ParameterTool params, EnrichmentsConfig enrichmentsConfig, String enrichmentType, DataStream<EnrichmentCommand> enrichmentSource, StreamExecutionEnvironment env) {
        enrichmentSource.addSink(sink);
    }

    private String getJsonPath(String testFileName) {
        File file = new File("src/test/resources/".concat(testFileName));
        return file.getAbsolutePath();
    }
}
