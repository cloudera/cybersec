/*
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

package com.cloudera.cyber.enrichemnt.stellar;

import com.cloudera.cyber.enrichemnt.stellar.functions.GeoEnrichmentFunctions;
import com.cloudera.cyber.enrichment.geocode.IpGeoJob;
import com.google.common.collect.ImmutableMap;
import org.apache.metron.enrichment.adapters.maxmind.asn.GeoLiteAsnDatabase;
import org.apache.metron.stellar.common.StellarProcessor;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.DefaultVariableResolver;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AsnEnrichmentFunctionsTest {

  private static Context context;
  private static File asnHdfsFile;

  private static final JSONObject expectedMessage = new JSONObject();
  private static final JSONObject expectedSubsetMessage = new JSONObject();

  private static final String TEST_IP = "1.128.0.0";
  private static final String TEST_ORG = "Telstra Pty Ltd";
  private static final String TEST_NUMBER = "1221";
  private static final String TEST_NETWORK = "1.128.0.0/11";

  @BeforeClass
  public static void setupOnce() {
    expectedMessage.put("autonomous_system_organization", TEST_ORG);
    expectedMessage.put("autonomous_system_number", TEST_NUMBER);
    expectedMessage.put("network", TEST_NETWORK);

    expectedSubsetMessage.put("autonomous_system_organization", TEST_ORG);
    expectedSubsetMessage.put("autonomous_system_number", TEST_NUMBER);

    String file = GeoEnrichmentFunctions.class.getResource("/GeoLite/GeoLite2-ASN-Test.mmdb").getFile();
    asnHdfsFile = new File(file);
  }

  @Before
  public void setup() {
    context = new Context.Builder().with(Context.Capabilities.GLOBAL_CONFIG,
        () -> ImmutableMap.of(IpGeoJob.PARAM_ASN_DATABASE_PATH, asnHdfsFile.getAbsolutePath())
    ).build();
  }

  public Object run(String rule, Map<String, Object> variables) {
    StellarProcessor processor = new StellarProcessor();
    assertTrue(processor.validate(rule, context), rule + " not valid.");
      return processor.parse(rule,
        new DefaultVariableResolver(variables::get, variables::containsKey),
        StellarFunctions.FUNCTION_RESOLVER(), context);
  }

  @Test
  public void testMissingDb() {
    context = new Context.Builder().with(Context.Capabilities.GLOBAL_CONFIG,
        () -> ImmutableMap.of(GeoLiteAsnDatabase.ASN_HDFS_FILE, "./fakefile.mmdb")
    ).build();
    String stellar = "ASN_GET()";
    try {
      run(stellar, ImmutableMap.of());
    } catch (Exception expected) {
      assertTrue(expected.getMessage().contains("File fakefile.mmdb does not exist"));
    }
  }

  @Test
  public void testMissingDbDuringUpdate() {
    String stellar = "ASN_GET()";
    Object result = run(stellar, ImmutableMap.of());
    assertNull(result, "Null IP should return null");
    try {
      GeoLiteAsnDatabase.INSTANCE.updateIfNecessary(
          Collections.singletonMap(GeoLiteAsnDatabase.ASN_HDFS_FILE, "./fakefile.mmdb"));
    } catch (IllegalStateException e) {
      // ignore it, the file doesn't exist
    }
    // Should still continue to query the old database, instead of dying.
    result = run(stellar, ImmutableMap.of());
    assertNull(result, "Null IP should return null");
  }

  @Test
  public void testGetEmpty() {
    String stellar = "ASN_GET()";
    Object result = run(stellar, ImmutableMap.of());
    assertNull(result, "Empty IP should return null");
  }

  @Test
  public void testGetNull() {
    String stellar = "ASN_GET(null)";
    Object result = run(stellar, ImmutableMap.of());
    assertNull(result, "Null IP should return null");
  }

  @Test
  public void testGetUndefined() {
    String stellar = "ASN_GET(undefined)";
    assertThrows(org.apache.metron.stellar.dsl.ParseException.class, () -> run(stellar, ImmutableMap.of()));
  }

  @Test
  public void testGetEmptyString() {
    String stellar = "ASN_GET('  ')";
    Object result = run(stellar, ImmutableMap.of());
    assertNull(result, "Empty IP should return null");
  }

  @Test
  public void testGetLocal() {
    String stellar = "ASN_GET('192.168.0.1')";
    Object result = run(stellar, ImmutableMap.of());
    assertEquals(new HashMap<String, String>(), result, "Local IP should return empty map");
  }

  @Test
  public void testGetRemote() {
    String stellar = "ASN_GET('1.128.0.0')";
    Object result = run(stellar, ImmutableMap.of());
    assertEquals(expectedMessage, result, "Remote IP should return result based on DB");
  }

  @Test
  public void testGetRemoteSingleField() {
    String stellar = String.format("ASN_GET('%s', ['autonomous_system_organization'])", TEST_IP);
    Object result = run(stellar, ImmutableMap.of());
    assertEquals(TEST_ORG, result, "Remote IP should return country result based on DB");
  }

  @Test
  public void testGetRemoteSingleFieldInteger() {
    String stellar = String.format("ASN_GET('%s', ['autonomous_system_number'])", TEST_IP);
    Object result = run(stellar, ImmutableMap.of());
    assertEquals(TEST_NUMBER, result, "Remote IP should return country result based on DB");
  }

  @Test
  public void testGetRemoteMultipleFields() {
    String stellar = String.format("ASN_GET('%s', ['autonomous_system_organization', 'autonomous_system_number'])", TEST_IP);
    Object result = run(stellar, ImmutableMap.of());
    assertEquals(expectedSubsetMessage, result,
            "Remote IP should return country result based on DB");
  }

  @Test
  public void testGetTooManyParams() {
    String stellar = String.format("ASN_GET('%s', ['autonomous_system_organization', 'autonomous_system_number', 'network'], 'garbage')", TEST_IP);
    assertThrows(org.apache.metron.stellar.dsl.ParseException.class, () -> run(stellar, ImmutableMap.of()));
  }
}
