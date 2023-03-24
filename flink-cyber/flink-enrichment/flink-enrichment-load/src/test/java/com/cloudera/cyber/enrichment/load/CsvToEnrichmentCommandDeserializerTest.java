package com.cloudera.cyber.enrichment.load;

import com.cloudera.cyber.EnrichmentEntry;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.commands.CommandType;
import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentConfig;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentFieldsConfig;
import com.google.common.collect.ImmutableMap;
import org.apache.flink.api.common.io.InputStreamFSInputWrapper;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.file.src.reader.StreamFormat;
import org.apache.flink.formats.csv.CsvReaderFormat;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

public class CsvToEnrichmentCommandDeserializerTest {

    private static final String TEST_ENRICHMENT_NAME = "TestEnrichment";

    // define majestic million enrichment config - one key field - one value field
    private static final ArrayList<String> MM_KEYS = new ArrayList<>(Collections.singletonList("domain"));
    private static final String MM_VALUE_FIELD = "rank";
    private static final ArrayList<String> MM_VALUES = new ArrayList<>(Collections.singletonList(MM_VALUE_FIELD));
    private static final EnrichmentFieldsConfig MM_FIELDS_CONFIG = new EnrichmentFieldsConfig(MM_KEYS, ":", MM_VALUES, null);
    private static final EnrichmentConfig MAJESTIC_MILLION_ENRICHMENT_CONFIG = new EnrichmentConfig("default", MM_FIELDS_CONFIG);

    // define threat indicator enrichment config - multiple key fields - multiple value fields
    private static final ArrayList<String> TI_KEYS = new ArrayList<>(Arrays.asList("indicator", "indicator_type"));
    private static final String TI_SOURCE_FIELD = "source";
    private static final String TI_SCORE_FIELD = "score";
    private static final ArrayList<String> TI_VALUES = new ArrayList<>(Arrays.asList(TI_SOURCE_FIELD, TI_SCORE_FIELD));
    private static final EnrichmentFieldsConfig TI_FIELDS_CONFIG = new EnrichmentFieldsConfig(TI_KEYS, ":", TI_VALUES, null);
    private static final EnrichmentConfig TI_ENRICHMENT_CONFIG = new EnrichmentConfig("default", TI_FIELDS_CONFIG);


    @Test
    public void testSingleLineWithSingleKeyAndValue() throws IOException {

        String columns = "rank,TldRank,domain,TLD,RefSubNets,RefIPs,IDN_Domain,IDN_TLD,PrevGlobalRank,PrevTldRank,PrevRefSubNets,PrevRefIPs";

        List<EnrichmentCommand> expectedCommands = Collections.singletonList(EnrichmentCommand.builder().
                type(CommandType.ADD).
                payload(EnrichmentEntry.builder().
                        type(TEST_ENRICHMENT_NAME).
                        key("facebook.com").
                        entries(ImmutableMap.of(MM_VALUE_FIELD, "1")).ts(MessageUtils.getCurrentTimestamp()).build()).
                build());
        testCSVFormat(false, columns, MAJESTIC_MILLION_ENRICHMENT_CONFIG, "1,1,facebook.com,com,494155,2890130,facebook.com,com,1,1,493314,2868293\n", expectedCommands );

    }

    @Test
    public void testMultiLineWithSingleKeyAndValue() throws IOException {

        String columns = "rank,TldRank,domain,TLD,RefSubNets,RefIPs,IDN_Domain,IDN_TLD,PrevGlobalRank,PrevTldRank,PrevRefSubNets,PrevRefIPs";

        String csv = "1,1,facebook.com,com,494155,2890130,facebook.com,com,1,1,493314,2868293\n" +
                     "5,5,instagram.com,com,354772,1748958,instagram.com,com,5,5,353741,1732258\n";

        List<EnrichmentCommand>  expectedCommands = new ArrayList<>();
        expectedCommands.add(EnrichmentCommand.builder().type(CommandType.ADD).payload(EnrichmentEntry.builder().
                type(TEST_ENRICHMENT_NAME).key("facebook.com").entries(ImmutableMap.of(MM_VALUE_FIELD, "1")).ts(MessageUtils.getCurrentTimestamp()).build()).build());
        expectedCommands.add(EnrichmentCommand.builder().type(CommandType.ADD).payload(EnrichmentEntry.builder().
                type(TEST_ENRICHMENT_NAME).key("instagram.com").entries(ImmutableMap.of(MM_VALUE_FIELD, "5")).ts(MessageUtils.getCurrentTimestamp()).build()).build());
        testCSVFormat(false, columns, MAJESTIC_MILLION_ENRICHMENT_CONFIG, csv, expectedCommands );
    }

    @Test
    public void testMultiLineWithMinimalColumnNames() throws IOException {

        String columns = "rank,,domain,,,,,,,,,";

        String csv = "1,1,facebook.com,com,494155,2890130,facebook.com,com,1,1,493314,2868293\n" +
                "5,5,instagram.com,com,354772,1748958,instagram.com,com,5,5,353741,1732258\n";

        List<EnrichmentCommand>  expectedCommands = new ArrayList<>();
        expectedCommands.add(EnrichmentCommand.builder().type(CommandType.ADD).payload(EnrichmentEntry.builder().
                type(TEST_ENRICHMENT_NAME).key("facebook.com").entries(ImmutableMap.of(MM_VALUE_FIELD, "1")).ts(MessageUtils.getCurrentTimestamp()).build()).build());
        expectedCommands.add(EnrichmentCommand.builder().type(CommandType.ADD).payload(EnrichmentEntry.builder().
                type(TEST_ENRICHMENT_NAME).key("instagram.com").entries(ImmutableMap.of(MM_VALUE_FIELD, "5")).ts(MessageUtils.getCurrentTimestamp()).build()).build());
        testCSVFormat(false, columns, MAJESTIC_MILLION_ENRICHMENT_CONFIG, csv, expectedCommands );
    }

    @Test
    public void testMultiLineSkipHeader() throws IOException {

        String columns = "rank,,domain,,,,,,,,,";

        String csv = columns + "\n" +
                "1,1,facebook.com,com,494155,2890130,facebook.com,com,1,1,493314,2868293\n" +
                "5,5,instagram.com,com,354772,1748958,instagram.com,com,5,5,353741,1732258\n";

        List<EnrichmentCommand>  expectedCommands = new ArrayList<>();
        expectedCommands.add(EnrichmentCommand.builder().type(CommandType.ADD).payload(EnrichmentEntry.builder().
                type(TEST_ENRICHMENT_NAME).key("facebook.com").entries(ImmutableMap.of(MM_VALUE_FIELD, "1")).ts(MessageUtils.getCurrentTimestamp()).build()).build());
        expectedCommands.add(EnrichmentCommand.builder().type(CommandType.ADD).payload(EnrichmentEntry.builder().
                type(TEST_ENRICHMENT_NAME).key("instagram.com").entries(ImmutableMap.of(MM_VALUE_FIELD, "5")).ts(MessageUtils.getCurrentTimestamp()).build()).build());
        testCSVFormat(true, columns, MAJESTIC_MILLION_ENRICHMENT_CONFIG, csv, expectedCommands );
    }

    @Test
    public void testMultipleKeysAndValuesNoHeader() throws IOException {

        String columns = "source,indicator_type,indicator,score";

        String csv = columns + "\n" +
                "abuse,IPADDRESS,1.1.1.1,45.0\n" +
                "vendor,DOMAIN,this.is.a.bad.one.xxx,99.0\n";

        List<EnrichmentCommand>  expectedCommands = new ArrayList<>();
        expectedCommands.add(EnrichmentCommand.builder().type(CommandType.ADD).payload(EnrichmentEntry.builder().
                type(TEST_ENRICHMENT_NAME).key("1.1.1.1:IPADDRESS").entries(ImmutableMap.of(TI_SOURCE_FIELD, "abuse", TI_SCORE_FIELD, "45.0")).ts(MessageUtils.getCurrentTimestamp()).build()).build());
        expectedCommands.add(EnrichmentCommand.builder().type(CommandType.ADD).payload(EnrichmentEntry.builder().
                type(TEST_ENRICHMENT_NAME).key("this.is.a.bad.one.xxx:DOMAIN").entries(ImmutableMap.of(TI_SOURCE_FIELD, "vendor", TI_SCORE_FIELD, "99.0")).ts(MessageUtils.getCurrentTimestamp()).build()).build());
        testCSVFormat(true, columns, TI_ENRICHMENT_CONFIG, csv, expectedCommands );
    }

    private void testCSVFormat(boolean skipFirst, String columnString, EnrichmentConfig enrichmentConfig, String inputString, List<EnrichmentCommand> expectedCommands) throws IOException {
        String[] columns = columnString.split(",", -1);

        CsvReaderFormat<EnrichmentCommand> csvFormat =
                CsvToEnrichmentCommandDeserializer.createCsvReaderFormat(enrichmentConfig, Arrays.asList(columns), skipFirst, TEST_ENRICHMENT_NAME);

        StreamFormat.Reader<EnrichmentCommand> reader = csvFormat.createReader(new Configuration(), new InputStreamFSInputWrapper(new ByteArrayInputStream(inputString.getBytes())));

        for(EnrichmentCommand expectedCommand : expectedCommands) {
            EnrichmentCommand actualCommand = reader.read();
            Assert.assertEquals(expectedCommand.getType(), Objects.requireNonNull(actualCommand).getType());
            EnrichmentEntry expectedPayload = expectedCommand.getPayload();
            EnrichmentEntry actualPayload = actualCommand.getPayload();
            Assert.assertEquals(expectedPayload.getKey(), actualPayload.getKey());
            Assert.assertEquals(expectedPayload.getType(), actualPayload.getType());
            Assert.assertEquals(expectedPayload.getEntries(), actualPayload.getEntries());
            long timestampDelta = actualPayload.getTs() - expectedPayload.getTs();
            Assert.assertTrue( String.format("timestamp delta %d too large", timestampDelta), timestampDelta < 1000L);
        }
    }

}
