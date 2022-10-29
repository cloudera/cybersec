package com.cloudera.cyber.enrichemnt.stellar;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class StellarEnrichmentJobTest {

    @Test
    void loadFilesFail() {
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