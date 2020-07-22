package com.cloudera.cyber.libs.hostnames;

import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public class TestNsLookup {

    @Test
    public void testDns() {
        List<NsLookupRecord> results = new NsLookup().eval("www.google.com", "AAAA");
        assertThat(results, hasSize(greaterThan(0)));
    }
    @Test
    public void testDnsMx() {
        List<NsLookupRecord> results = new NsLookup().eval("google.com", "MX");
        assertThat(results, hasSize(greaterThan(0)));
    }
}
