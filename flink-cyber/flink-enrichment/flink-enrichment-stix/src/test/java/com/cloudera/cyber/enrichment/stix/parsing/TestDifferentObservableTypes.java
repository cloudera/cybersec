package com.cloudera.cyber.enrichment.stix.parsing;

import org.junit.Test;

import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsIterableContaining.hasItems;

public class TestDifferentObservableTypes extends AbstractStixParserTest {
    @Test
    public void testDomain() throws Exception {
        doTest("domain.xml", parsedThreatIntelligence -> {
            assertThat(parsedThreatIntelligence.threatIntelligence.getObservableType(), equalTo("URIObject:URIObjectType"));
            assertThat(parsedThreatIntelligence.threatIntelligence.getObservable(), notNullValue());
            assertThat(parsedThreatIntelligence.threatIntelligence.getStixReference(), notNullValue());
        }, all -> {
            assertThat("All observables output an entry", all.size(), equalTo(3));
            assertThat("All outputs are linked back to a common indicator reference",
                    all.stream().map(a -> a.getThreatIntelligence().getStixReference()).collect(Collectors.toSet()), hasSize(1));
            assertThat("Finds the relevant domains",
                    all.stream().map(d -> d.getThreatIntelligence().getObservable()).collect(Collectors.toList()),
                    hasItems("malicious1.example.com", "malicious2.example.com","malicious3.example.com"));
        });
    }


    @Test
    public void testDomain2() throws Exception {
        doTest("domain2.xml", parsedThreatIntelligence -> {
            assertThat(parsedThreatIntelligence.threatIntelligence.getObservableType(), equalTo("DomainNameObj:FQDN"));
            assertThat(parsedThreatIntelligence.threatIntelligence.getObservable(), notNullValue());
            assertThat(parsedThreatIntelligence.threatIntelligence.getStixReference(), notNullValue());
        }, all -> {
            assertThat("All observables output an entry", all.size(), equalTo(3));
            assertThat("All outputs are linked back to a common indicator reference",
                    all.stream().map(a -> a.getThreatIntelligence().getStixReference()).collect(Collectors.toSet()), hasSize(1));
            assertThat("Finds the relevant domains",
                    all.stream().map(d -> d.getThreatIntelligence().getObservable()).collect(Collectors.toList()),
                    hasItems("malicious1.example.com", "malicious2.example.com","malicious3.example.com"));
        });
    }

    @Test
    public void testDomainIp() throws Exception {
        doTest("ip.xml", parsedThreatIntelligence -> {
            assertThat(parsedThreatIntelligence.threatIntelligence.getObservableType(), equalTo("Address:ipv4-addr"));
            assertThat(parsedThreatIntelligence.threatIntelligence.getObservable(), notNullValue());
            assertThat(parsedThreatIntelligence.threatIntelligence.getStixReference(), notNullValue());
        }, all -> {
            assertThat("All observables output an entry", all.size(), equalTo(1));
            assertThat("All outputs are linked back to a common indicator reference",
                    all.stream().map(a -> a.getThreatIntelligence().getStixReference()).collect(Collectors.toSet()), hasSize(1));
            assertThat("Finds the relevant domains",
                    all.stream().map(d -> d.getThreatIntelligence().getObservable()).collect(Collectors.toList()),
                    hasItems("192.168.0.1"));
        });
    }
}
