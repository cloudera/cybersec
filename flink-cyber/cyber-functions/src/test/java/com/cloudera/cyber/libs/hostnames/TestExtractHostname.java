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
