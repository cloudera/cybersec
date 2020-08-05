package com.cloudera.cyber.enrichment.stix.parsing;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class TestSample extends AbstractStixParserTest {
    @Test
    public void testSample() throws Exception {
        doTest("sample.xml", parsedThreatIntelligence -> {
            assertThat(parsedThreatIntelligence.getThreatIntelligence().getObservableType(), Matchers.notNullValue());
            assertThat(parsedThreatIntelligence.getThreatIntelligence().getStixReference(), Matchers.notNullValue());
        }, all -> {
            assertThat("All observables output an entry", all.size(), equalTo(3));
            assertThat("All outputs are linked back to a common indicator reference",
                    all.stream().map(a -> a.getThreatIntelligence().getStixReference()).collect(Collectors.toSet()),
                    hasSize(1));
        });
    }
}
