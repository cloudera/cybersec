package com.cloudera.cyber.test;

import com.cloudera.cyber.test.generator.ThreatGenerator;
import freemarker.template.TemplateException;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class ThreatGeneratorTest {

    @Test
    public void testThreatGenerator() throws IOException, TemplateException {
        String message = new ThreatGenerator().generateThreat("10.0.0.1");
        assertThat(message, containsString("10.0.0.1"));
    }
}
