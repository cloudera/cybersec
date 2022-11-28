/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

package com.cloudera.cyber.enrichment.threatq;

import com.cloudera.cyber.EnrichmentEntry;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
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
