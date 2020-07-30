package com.cloudera.cyber.libs.hostnames;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;

public class TestExtractHostnameFeatures {
    @Test
    public void testExtractHostnameFeatures() {
        assertThat("Extracts from www.google.co.uk", new ExtractHostnameFeatures().eval("www.google.co.uk"),
                allOf(
                        hasEntry("TLD", "co.uk"),
                        hasEntry("NO_TLD", "www.google"),
                        hasEntry("NO_SUBS_NO_TLD", "google"),
                        hasEntry("NO_SUBS", "google.co.uk")
                ));

        assertThat("Extracts from test.aero", new ExtractHostnameFeatures().eval("test.aero"),
                allOf(
                        hasEntry("TLD", "aero"),
                        hasEntry("NO_TLD", "test"),
                        hasEntry("NO_SUBS_NO_TLD", "test"),
                        hasEntry("NO_SUBS", "test.aero")
                ));

        assertThat("Extracts from test.aero", new ExtractHostnameFeatures().eval("sub.test.aero"),
                allOf(
                        hasEntry("TLD", "aero"),
                        hasEntry("NO_TLD", "sub.test"),
                        hasEntry("NO_SUBS_NO_TLD", "test"),
                        hasEntry("NO_SUBS", "test.aero")
                ));


        assertThat("Works with reverse IP",  new ExtractHostnameFeatures().eval("1.0.0.10.in-addr.arpa"),
                allOf(
                        hasEntry("TLD", "in-addr.arpa"),
                        hasEntry("NO_TLD", "10.0.0.1"),
                        hasEntry("NO_SUBS_NO_TLD", "10.0.0.1"),
                        hasEntry("NO_SUBS", "10.0.0.1")
                ));
    }
}
