package com.cloudera.cyber.enrichment.load;

import com.cloudera.cyber.EnrichmentEntry;
import com.cloudera.cyber.commands.CommandType;
import com.cloudera.cyber.commands.EnrichmentCommand;
import com.google.common.collect.ImmutableMap;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.CollectingSink;
import org.apache.flink.test.util.JobTester;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class HbaseBatchEnrichmentCSVLoaderTest extends BatchEnrichmentLoaderCSV {


    private CollectingSink<EnrichmentCommand> sink = new CollectingSink<>();

    private static final String MAJESTIC_MILLION_CF = "majestic_million";
    private static final Map<String, String> firstExtensions = new HashMap<String,String>()
        {{
            put("GlobalRank", "1");
            put("TldRank", "1");
            put("TLD", "com");
            put("RefSubNets", "494155");
            put("RefIPs", "2890130");
            put("IDN_Domain", "facebook.com");
            put("IDN_TLD", "com");
            put("PrevGlobalRank", "1");
            put("PrevTldRank", "1");
            put("PrevRefSubNets", "493314");
            put("PrevRefIPs", "2868293");
        }};

    private static final Map<String, String> lastExtensions = new HashMap<String,String>()
        {{
            put("GlobalRank", "9");
            put("TldRank", "1");
            put("TLD", "org");
            put("RefSubNets", "285566");
            put("RefIPs", "1126132");
            put("IDN_Domain", "wikipedia.org");
            put("IDN_TLD", "org");
            put("PrevGlobalRank", "9");
            put("PrevTldRank", "1");
            put("PrevRefSubNets", "284262");
            put("PrevRefIPs", "1111606");
        }};

    private static final Map<String, Map<String, String>>  extensionsWithAllData =
            ImmutableMap.of("facebook.com", firstExtensions,
                    "wikipedia.org", lastExtensions);

    @Test
    public void testCSVLoad() throws Exception {
        testLoadStream("GlobalRank,TldRank,Domain,TLD,RefSubNets,RefIPs,IDN_Domain,IDN_TLD,PrevGlobalRank,PrevTldRank,PrevRefSubNets,PrevRefIPs", extensionsWithAllData);
    }

    @Test
    public void testCSVIgnoreColumns() throws Exception {
        Map<String, Map<String, String>> expectedReducedValues = extensionsWithAllData.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> ImmutableMap.of("GlobalRank", v.getValue().get("GlobalRank"))));
        testLoadStream("GlobalRank,,Domain,,,,,,,,,", expectedReducedValues);
    }

    private void testLoadStream(String enrichmentFields, Map<String, Map<String, String>> enrichentsToVerify) throws Exception {
        JobTester.startTest(runPipeline(ParameterTool.fromMap(ImmutableMap.of(
                ENRICHMENT_SOURCE_FILE, "./src/test/resources/majestic_million_sample.csv",
                ENRICHMENT_SKIP_FIRST_LINE, "true",
                ENRICHMENT_FIELDS, enrichmentFields,
                ENRICHMENT_KEY_FIELD_NAME, "Domain",
                ENRICHMENT_COLUMN_FAMILY, MAJESTIC_MILLION_CF
        ))));

        List<EnrichmentCommand> enrichmentCommands = new ArrayList<>();

        for(int i = 0; i < 9; i++) {
            enrichmentCommands.add(sink.poll());
        }
        JobTester.stopTest();
        verifyEnrichmentCommands(enrichentsToVerify, enrichmentCommands);
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
    protected void writeEnrichments(StreamExecutionEnvironment env, ParameterTool params, DataStream<EnrichmentCommand> enrichmentSource) {
        enrichmentSource.addSink(sink);
    }
}
