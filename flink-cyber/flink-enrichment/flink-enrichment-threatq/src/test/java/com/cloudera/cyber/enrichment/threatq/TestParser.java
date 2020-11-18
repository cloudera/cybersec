package com.cloudera.cyber.enrichment.threatq;

import com.cloudera.cyber.EnrichmentEntry;
import org.hamcrest.collection.IsMapContaining.*;
import org.hamcrest.collection.IsMapWithSize.*;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

public class TestParser {

    @Test
    public void testParser() throws IOException {
        Stream<ThreatQEntry> threatQEntry = ThreatQParser.parse(
                ClassLoader.getSystemResourceAsStream("sample.txt")
        );

        List<ThreatQEntry> output = threatQEntry.collect(Collectors.toList());
        assertThat(output, hasSize(12));

        output.forEach(o -> {
            assertThat(o, notNullValue());
            assertThat(o.getTq_id().toString(), o, hasProperty("tq_url"));
        });

        assertThat(output, hasItem(
                allOf(
                        hasProperty("tq_id", equalTo(82048L)),
                        hasProperty("tq_attributes",
                                allOf(aMapWithSize(2), hasEntry("Malware Type", "Heodo"))
                        )

                )
        ));
    }

    @Test
    public void testTranslateToEnrichment() throws IOException {
        List<EnrichmentEntry> collect = ThreatQParser.parse(
                ClassLoader.getSystemResourceAsStream("sample.txt")
        ).map(ThreatQEntry::toEnrichmentEntry).collect(Collectors.toList());

        assertThat(collect, allOf(
                hasSize(12),
                everyItem(
                        allOf(isA(EnrichmentEntry.class),
                                hasProperty("ts", allOf(isA(long.class))),
                                hasProperty("entries", hasKey("tq_id"))
                        ))
                )
        );
    }
}
