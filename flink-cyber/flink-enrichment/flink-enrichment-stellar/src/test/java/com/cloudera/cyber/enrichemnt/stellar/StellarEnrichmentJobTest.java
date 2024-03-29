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

package com.cloudera.cyber.enrichemnt.stellar;


import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class StellarEnrichmentJobTest {

    @Test
    public void loadFilesFail() {
        final String path = "Not-existing-path";
        try {
            StellarEnrichmentJob.loadFiles(path);
        } catch (Exception e) {
            final String message = String.format("Provided config directory doesn't exist or empty [%s]!", path);
            assertThat("Exception message not expected", e.getMessage(), is(message));
            assertThat("Exception type not expected", e.getClass(), is(RuntimeException.class));
        }
    }
}