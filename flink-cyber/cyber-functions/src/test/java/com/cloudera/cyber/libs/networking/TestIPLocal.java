package com.cloudera.cyber.libs.networking;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class TestIPLocal {
    @Test
    public void testIPLocal() {
        IPLocal func = new IPLocal();

        assertTrue(func.eval("192.168.0.1"));
        assertTrue(func.eval("192.168.1.1"));
        assertTrue(func.eval("172.16.0.1"));
        assertTrue(func.eval("172.24.0.1"));
        assertTrue(func.eval("10.0.0.1"));
        assertTrue(func.eval("10.254.254.254"));
        assertFalse(func.eval("192.169.0.1"));
        assertFalse(func.eval("8.8.8.8"));
    }
}