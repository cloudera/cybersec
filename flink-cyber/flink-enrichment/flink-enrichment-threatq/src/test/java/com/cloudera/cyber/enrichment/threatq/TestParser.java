package com.cloudera.cyber.enrichment.threatq;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

@RunWith(BlockJUnit4ClassRunner.class)
public class TestParser {

    @Test
    public void testParser() throws IOException {
        Stream<ThreatQEntry> threatQEntry = ThreatQParser.parse(
          ClassLoader.getSystemResourceAsStream("sample.txt")
        );

        List<ThreatQEntry> output = threatQEntry.collect(Collectors.toList());
        assertThat(output, hasSize(12));
    }
}
