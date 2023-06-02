package com.cloudera.cyber.generator;

import org.junit.Assert;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GenerationSourceTest {

    @Test
    public void testReadSchema() throws IOException {
        verifySchemaRead("test.schema", false);
        verifySchemaRead("src/test/resources/test.schema", false);
        verifySchemaRead(null, true);
    }

    @Test
    public void testSchemaDoesNotExist() {
        assertThatThrownBy(() -> verifySchemaRead("doesnt_exist", false)).isInstanceOf(FileNotFoundException.class)
                .hasMessage("Basedir: '' File: 'doesnt_exist'");
    }

    private void verifySchemaRead(String schemaPath, boolean isNull) throws IOException {
        String test_template = "test_template";
        String test_topic = "test_topic";
        GenerationSource source = new GenerationSource(test_template, test_topic, schemaPath, 1.0);

        source.readAvroSchema("");
        Assert.assertEquals(isNull, source.getOutputAvroSchema() == null);
    }

    @Test
    public void testScenarioFileParameters() throws IOException {

        Set<String> expectedKeys = new HashSet<>();
        expectedKeys.add("ip_src_addr");
        expectedKeys.add("url");

        testScenarioFile("src/test/resources/config/squid.csv", expectedKeys);
        testScenarioFile(null, Collections.emptySet());
    }

    @Test
    public void testScenarioFileDoesntExist() {
        assertThatThrownBy(() -> testScenarioFile("doesnt_exist.csv", Collections.emptySet())).isInstanceOf(FileNotFoundException.class)
                .hasMessage("Basedir: '' File: 'doesnt_exist.csv'");
    }

    private void testScenarioFile(String scenarioFilePath, Set<String> expectedKeys) throws IOException {
        GenerationSource gs = new GenerationSource("file", "topic", null, 1.0, scenarioFilePath, null,  null );
        gs.readScenarioFile("");

        Assert.assertEquals(scenarioFilePath == null, gs.getScenario() == null);

        Map<String, String> randomParameters = gs.getRandomParameters();

        Assert.assertEquals(expectedKeys, randomParameters.keySet());
    }
}
