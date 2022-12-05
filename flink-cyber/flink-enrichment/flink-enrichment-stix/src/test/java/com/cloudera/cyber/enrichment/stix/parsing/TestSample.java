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
