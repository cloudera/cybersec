package com.cloudera.cyber.generator.scenario;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GeneratorScenarioTest {
    private static final String IP_DEST_ADDR = "ip_dst_addr";
    private static final String DOMAIN_NAME = "domain_name";
    private static final String ACTION = "action";

    @Test
    public void testCsvScenario() throws IOException {
        GeneratorScenario scenario = GeneratorScenario.load("src/test/resources/scenario.csv");

        Map<String,String> expectedValue1 = ImmutableMap.of(IP_DEST_ADDR, "1.1.1.1", DOMAIN_NAME, "www.google.com", ACTION, "CONNECT");
        Map<String,String> expectedValue2 = ImmutableMap.of(IP_DEST_ADDR, "2.2.2.2", DOMAIN_NAME, "www.amazon.com", ACTION, "MISS");
        List<Map<String, String>> expectedValues = Lists.newArrayList(expectedValue1, expectedValue2);

        List<Map<String,String>> actualRandomParameters = new ArrayList<>();
        Set<String> actualIpDstAddrValues = new HashSet<>();

        int count = 100;
        for(int i = 0; i < count; i++) {
            Map<String, String> randomParameters = scenario.randomParameters();
            Assert.assertTrue(String.format("randomParameters = %s does not match expected values", randomParameters), expectedValues.contains(randomParameters));
            actualIpDstAddrValues.add(randomParameters.get(IP_DEST_ADDR));
            actualRandomParameters.add(randomParameters);
        }
        Assert.assertEquals(count, actualRandomParameters.size());
        Assert.assertEquals(2, actualIpDstAddrValues.size());
    }

    @Test
    public void testCsvFileNotFound() {
        String notFoundFileName = "doesnt_exist.csv";
        assertThatThrownBy(() ->GeneratorScenario.load(notFoundFileName)).isInstanceOf(IOException.class).
                hasMessage(String.format("%s (No such file or directory)", notFoundFileName));
    }

    @Test
    public void testEmptyFile() {
        testNotEnoughLines("src/test/resources/scenario_empty.csv");
    }

    @Test
    public void testHeaderOnlyFile() {
        testNotEnoughLines("src/test/resources/scenario_header_only.csv");
    }

    private void testNotEnoughLines(String scenarioFile) {
        assertThatThrownBy(() ->GeneratorScenario.load(scenarioFile)).
                isInstanceOf(IllegalStateException.class).
                hasMessage(GeneratorScenario.NOT_ENOUGH_LINES_ERROR);

    }
}
