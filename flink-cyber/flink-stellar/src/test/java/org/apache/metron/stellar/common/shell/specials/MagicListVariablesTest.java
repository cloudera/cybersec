/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.metron.stellar.common.shell.specials;

import org.apache.metron.stellar.common.shell.DefaultStellarShellExecutor;
import org.apache.metron.stellar.common.shell.StellarResult;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class MagicListVariablesTest {

  MagicListVariables magic;
  DefaultStellarShellExecutor executor;

  @BeforeEach
  public void setup() throws Exception {

    // setup the %magic
    magic = new MagicListVariables();

    // setup the executor
    Properties props = new Properties();
    executor = new DefaultStellarShellExecutor(props, Optional.empty());
    executor.init();
  }

  @Test
  public void testGetCommand() {
    assertEquals("%vars", magic.getCommand());
  }

  @Test
  public void testShouldMatch() {
    List<String> inputs = Arrays.asList(
            "%vars",
            "   %vars   ",
            "%vars   FOO",
            "    %vars    FOO "
    );
    for(String in : inputs) {
      assertTrue(magic.getMatcher().apply(in), "failed: " + in);
    }
  }

  @Test
  public void testShouldNotMatch() {
    List<String> inputs = Arrays.asList(
            "foo",
            "  vars ",
            "bar",
            "%define"
    );
    for(String in : inputs) {
      assertFalse(magic.getMatcher().apply(in), "failed: " + in);
    }
  }

  @Test
  public void test() {
    // define some vars
    executor.execute("x := 2 + 2");
    StellarResult result = executor.execute("%vars");

    // validate the result
    assertTrue(result.isSuccess());
    assertTrue(result.getValue().isPresent());

    // validate the list of vars
    String vars = ConversionUtils.convert(result.getValue().get(), String.class);
    assertEquals("x = 4 via `2 + 2`", vars);
  }

  @Test
  public void testWithNoVars() {
    // there are no vars defined
    StellarResult result = executor.execute("%vars");

    // validate the result
    assertTrue(result.isSuccess());
    assertTrue(result.getValue().isPresent());

    // validate the list of vars
    String vars = ConversionUtils.convert(result.getValue().get(), String.class);
    assertEquals("", vars);
  }
}
