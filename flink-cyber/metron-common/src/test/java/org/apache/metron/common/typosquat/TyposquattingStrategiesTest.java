/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.common.typosquat;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.apache.metron.stellar.common.utils.StellarProcessorUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TyposquattingStrategiesTest {

  /*
  Skipping the miscellaneous typosquatted domains as they rely on the TLD.
   */
  static Set<String> typesToSkip = new HashSet<String>() {{
    add("Various");
    add("Original*");
  }};

  //These are the output from DNS Twist (  https://github.com/elceef/dnstwist ).  We want to ensure we match their output at minimum
  static Map<String, EnumMap<TyposquattingStrategies, Set<String>>> expected = new HashMap<String, EnumMap<TyposquattingStrategies, Set<String>>>()
  {{
    put("amazon", new EnumMap<>(TyposquattingStrategies.class));
    put("github", new EnumMap<>(TyposquattingStrategies.class));
  }};

  @BeforeAll
  public static void setup() throws Exception {
    for(Map.Entry<String, EnumMap<TyposquattingStrategies, Set<String>>> kv : expected.entrySet()) {
      try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("src/test/resources/typosquat/" + kv.getKey() + ".csv"), StandardCharsets.UTF_8) ) )
      {
        for(String line = null;(line = br.readLine()) != null;) {
          if(line.startsWith("#")) {
            continue;
          }
          Iterable<String> tokens = Splitter.on(",").split(line);
          String name = Iterables.get(tokens, 0);
          String domain = Iterables.get(tokens, 1);
          domain = domain.replaceAll(".com", "");
          EnumMap<TyposquattingStrategies, Set<String>> expectedValues = kv.getValue();
          if(typesToSkip.contains(name)) {
            continue;
          }
          TyposquattingStrategies strategy = TyposquattingStrategies.byName(name);
          assertNotNull(strategy, "Couldn't find " + name);
          Set<String> s = expectedValues.get(strategy);
          if(s == null) {
            s = new HashSet<>();
            expectedValues.put(strategy, s);
          }
          s.add(domain);
        }
      }
    }
  }

  public void assertExpected(String domain, TyposquattingStrategies strategy) {
    Set<String> expectedValues = expected.get(domain).get(strategy);
    Set<String> actualValues = strategy.generateCandidates(domain);
    assertFalse(actualValues.contains(domain));
    {
      Sets.SetView<String> vals = Sets.difference(expectedValues, actualValues);
      String diff = Joiner.on(",").join(vals);
      assertTrue(vals.isEmpty(), strategy.name() + ": Found values expected but not generated: " + diff);
    }
  }

  public static TyposquattingStrategies[] strategies() {
    return TyposquattingStrategies.values();
  }

  @ParameterizedTest
  @MethodSource("strategies")
  public void test(TyposquattingStrategies strategy) {
    for(String domain : expected.keySet()) {
      assertExpected(domain, strategy);
    }
  }

  @Test
  public void testStellar() {
    for(String domain : expected.keySet()) {
      Set<String> expectedAll = TyposquattingStrategies.generateAllCandidates(domain);
      Set<String> generatedAll = (Set<String>) StellarProcessorUtils.run("DOMAIN_TYPOSQUAT(domain)", ImmutableMap.of("domain", domain));
      assertTrue(Sets.symmetricDifference(expectedAll, generatedAll).isEmpty());
    }
  }
}
