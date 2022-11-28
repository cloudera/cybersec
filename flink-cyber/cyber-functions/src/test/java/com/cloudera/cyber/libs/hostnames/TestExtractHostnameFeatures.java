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
