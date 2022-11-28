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