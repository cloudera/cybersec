package com.cloudera.cyber.libs;

import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public class TestCyberFunctionUtils {

    @Test
    public void testClassFinder() {
        List<Class<?>> allFunctions = CyberFunctionUtils.findAll().collect(Collectors.toList());
        assertThat("Returns some functions", allFunctions, hasSize(greaterThan(1)));
    }
}
