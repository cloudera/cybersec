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

package org.apache.metron.common.field.transformation;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.common.configuration.FieldTransformer;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.stellar.dsl.Context;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FieldTransformationTest {
  public static class TestTransformation implements FieldTransformation {

    @Override
    public Map<String, Object> map( Map<String, Object> input
                                  , List<String> outputField
                                  , LinkedHashMap<String, Object> fieldMappingConfig
                                  , Context context
                                  , Map<String, Object>... sensorConfig
                                  )
    {
      return ImmutableMap.of(outputField.get(0), Joiner.on(fieldMappingConfig.get("delim").toString()).join(input.entrySet()));
    }
  }

 /**
   {
    "fieldTransformations" : [
          {
            "input" : [ "field1", "field2" ]
          , "output" : "output"
          , "transformation" : "org.apache.metron.common.field.transformation.FieldTransformationTest$TestTransformation"
          , "config" : {
                "delim" : ","
                      }
          }
                      ]
   }
   */
  @Multiline
  public static String complexConfig;

  /**
   {
    "fieldTransformations" : [
          {
            "input" : "protocol"
          , "transformation" : "IP_PROTOCOL"
          }
                      ]
   }
   */
  @Multiline
  public static String config;

  /**
   {
    "fieldTransformations" : [
          {
            "input" : "protocol"
          }
                      ]
   }
   */
  @Multiline
  public static String badConfigMissingMapping;

  @Test
  public void testValidSerde_simple() throws IOException {
    SensorParserConfig c = SensorParserConfig.fromBytes(Bytes.toBytes(config));
    assertEquals(1, c.getFieldTransformations().size());
    assertEquals(IPProtocolTransformation.class, c.getFieldTransformations().get(0).getFieldTransformation().getClass());
    assertEquals(ImmutableList.of("protocol"), c.getFieldTransformations().get(0).getInput());
  }



  @Test
  public void testInValidSerde_missingMapping() {
    assertThrows(IllegalStateException.class, () -> SensorParserConfig.fromBytes(Bytes.toBytes(badConfigMissingMapping)));
  }

  @Test
  public void testComplexMapping() throws IOException {
    SensorParserConfig c = SensorParserConfig.fromBytes(Bytes.toBytes(complexConfig));
    FieldTransformer handler = Iterables.getFirst(c.getFieldTransformations(), null);

    assertNotNull(handler);
    assertEquals(ImmutableMap.of("output", "field1=value1,field2=value2")
                       ,handler.transform(new JSONObject(ImmutableMap.of("field1", "value1"
                                                                  ,"field2", "value2"
                                                                  )
                                                  )
                                   , Context.EMPTY_CONTEXT()
                                   , c.getParserConfig()
                                   )
                       );
  }
  @Test
  public void testSimpleMapping() throws IOException {
    SensorParserConfig c = SensorParserConfig.fromBytes(Bytes.toBytes(config));
    FieldTransformer handler = Iterables.getFirst(c.getFieldTransformations(), null);

    assertNotNull(handler);
    assertEquals(ImmutableMap.of("protocol", "TCP")
                       ,handler.transform(new JSONObject(ImmutableMap.of("protocol", 6))
                                         , Context.EMPTY_CONTEXT()
                                         , c.getParserConfig()
                                         )
                       );
  }
}
