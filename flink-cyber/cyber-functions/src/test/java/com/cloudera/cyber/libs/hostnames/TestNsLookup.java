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
