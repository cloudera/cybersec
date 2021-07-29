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

package org.apache.metron.common.field.validation;

import com.google.common.collect.ImmutableList;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.configuration.Configurations;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class ValidationTest extends BaseValidationTest{
  /**
   {
    "fieldValidations" : [
            {
              "input" : "field1"
             ,"validation" : "NOT_EMPTY"
            }
                         ]
   }
   */
  @Multiline
  public static String validValidationConfigWithStringInput;

  /**
   {
    "fieldValidations" : [
            {
              "input" : [ "field1", "field2" ]
             ,"validation" : "NOT_EMPTY"
            }
                         ]
   }
   */
  @Multiline
  public static String validValidationConfigWithListInput;
  /**
   {
    "fieldValidations" : [
            {
              "input" : "field1"
            }
                         ]
   }
   */
  @Multiline
  public static String invalidValidationConfig;
  @Test
  public void testValidConfiguration() throws IOException {
    {
      Configurations configurations = getConfiguration(validValidationConfigWithStringInput);
      assertNotNull(configurations.getFieldValidations());
      assertEquals(1, configurations.getFieldValidations().size());
      assertEquals(ImmutableList.of("field1"), configurations.getFieldValidations().get(0).getInput());
    }
    {
      Configurations configurations = getConfiguration(validValidationConfigWithListInput);
      assertNotNull(configurations.getFieldValidations());
      assertEquals(1, configurations.getFieldValidations().size());
      assertEquals(ImmutableList.of("field1", "field2"), configurations.getFieldValidations().get(0).getInput());
    }
  }

  @Test
  public void testInvalidConfiguration() {
    assertThrows(IllegalStateException.class, () -> getConfiguration(invalidValidationConfig));
  }

}
