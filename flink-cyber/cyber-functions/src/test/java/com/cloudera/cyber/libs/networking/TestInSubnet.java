package com.cloudera.cyber.libs.networking;

import org.junit.Test;

import java.sql.Types;

import static org.hamcrest.Matchers.isA;
import static org.junit.Assert.*;

public class TestInSubnet {

    @Test
    public void testInSubnet() {
        InSubnet func = new InSubnet();

        assertTrue(func.eval("192.168.0.1", "192.168.0.0/24"));
        assertFalse(func.eval("192.168.1.1", "192.168.0.0/24"));
        assertTrue(func.eval("192.168.1.1", "192.168.0.0/16"));
        assertTrue(func.eval("192.168.0.1", "0.0.0.0/0"));
        assertFalse(func.eval("192.168.1.1", "10.0.0.0/8"));
    }
}
