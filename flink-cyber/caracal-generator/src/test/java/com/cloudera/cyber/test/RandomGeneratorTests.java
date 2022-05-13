package com.cloudera.cyber.test;

import com.cloudera.cyber.generator.RandomGenerators;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RandomGeneratorTests {

    @Test
    public void testCanoncialName() {
        assertEquals("www.extra.cloudera.com",
                RandomGenerators.canonicalize("www.cloudera.com"));
    }
}
