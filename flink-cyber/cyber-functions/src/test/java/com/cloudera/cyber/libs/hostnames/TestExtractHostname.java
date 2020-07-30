package com.cloudera.cyber.libs.hostnames;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class TestExtractHostname {

    @Test
    public void testExtractHostnameTLD() {
        assertThat(new ExtractHostname().eval("www.google.co.uk", ExtractHostname.HostnameFeature.TLD), equalTo("co.uk"));
        assertThat(new ExtractHostname().eval("1.0.0.10.in-addr.arpa", ExtractHostname.HostnameFeature.TLD), equalTo("in-addr.arpa"));
    }

    @Test
    public void testExtractHostnameNO_TLD() {
        assertThat(new ExtractHostname().eval("www.google.co.uk", ExtractHostname.HostnameFeature.NO_TLD), equalTo("www.google"));
        assertThat(new ExtractHostname().eval("1.0.0.10.in-addr.arpa", ExtractHostname.HostnameFeature.NO_TLD), equalTo("10.0.0.1"));
    }

    @Test
    public void testExtractHostnameNO_SUBS_NO_TLD() {
        assertThat(new ExtractHostname().eval("www.google.co.uk", ExtractHostname.HostnameFeature.NO_SUBS_NO_TLD), equalTo("google"));
        assertThat(new ExtractHostname().eval("1.0.0.10.in-addr.arpa", ExtractHostname.HostnameFeature.NO_SUBS_NO_TLD), equalTo("10.0.0.1"));
    }

    @Test
    public void testExtractHostnameNO_SUBS() {
        assertThat(new ExtractHostname().eval("www.google.co.uk", ExtractHostname.HostnameFeature.NO_SUBS), equalTo("google.co.uk"));
        assertThat(new ExtractHostname().eval("1.0.0.10.in-addr.arpa", ExtractHostname.HostnameFeature.NO_SUBS), equalTo("10.0.0.1"));
    }

}
