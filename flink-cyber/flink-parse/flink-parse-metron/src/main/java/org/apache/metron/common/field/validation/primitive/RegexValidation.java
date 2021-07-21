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

package org.apache.metron.common.field.validation.primitive;

import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.common.field.validation.FieldValidation;

import java.util.Map;

public class RegexValidation implements FieldValidation {

  private enum Config {
    REGEX("pattern")
    ;
    String key;
    Config(String key) {
      this.key = key;
    }
    public <T> T get(Map<String, Object> config, Class<T> clazz) {
      Object o = config.get(key);
      if(o == null) {
        return null;
      }
      return clazz.cast(o);
    }
  }

  @Override
  public boolean isValid( Map<String, Object> input
                        , Map<String, Object> validationConfig
                        , Map<String, Object> globalConfig
                        , Context context
                        ) {

    String regex = Config.REGEX.get(validationConfig, String.class);
    if(regex == null) {
      return false;
    }
    for(Object o : input.values()) {
      if(o != null && !o.toString().matches(regex)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void initialize(Map<String, Object> validationConfig, Map<String, Object> globalConfig) {
    String regex = Config.REGEX.get(validationConfig, String.class);
    if(regex == null) {
      throw new IllegalStateException("You must specify '" + Config.REGEX.key + "' in the config");
    }
  }
}
