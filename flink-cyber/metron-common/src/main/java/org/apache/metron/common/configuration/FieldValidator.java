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

package org.apache.metron.common.configuration;

import com.google.common.collect.ImmutableList;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.metron.common.field.validation.FieldValidation;
import org.apache.metron.common.field.validation.FieldValidations;
import org.apache.metron.stellar.dsl.Context;
import org.json.simple.JSONObject;

/**
 * Allows for the ability to run validations across messages that are being passed through the
 * system.
 */
public class FieldValidator implements Serializable {

  public enum Config {
     FIELD_VALIDATIONS("fieldValidations")
    ,VALIDATION("validation")
    ,INPUT("input")
    ,CONFIG("config")
    ;
    String key;
    Config(String key) {
      this.key = key;
    }

    /**
     * Retrieves the value of the key from the provided config, and casts it to the provided class.
     *
     * @param config The config to retrieve the value associated with the key from
     * @param clazz The class to cast to
     * @param <T> The type parameter of the class to cast to
     * @return The value, casted appropriately
     */
    public <T> T get(Map<String, Object> config, Class<T> clazz) {
      Object o = config.get(key);
      if(o == null) {
        return null;
      }
      return clazz.cast(o);
    }
  }
  private FieldValidation validation;
  private List<String> input;
  private Map<String, Object> config;

  /**
   * Constructor for a FieldValidator.
   *
   * @param o Should be a map, otherwise exception is thrown. From the map, retrieve the various
   *     necessary components, e.g. input, and ensure they are read appropriately.
   *
   */
  public FieldValidator(Object o) {
    if(o instanceof Map) {
      Map<String, Object> validatorConfig = (Map<String, Object>) o;
      Object inputObj = Config.INPUT.get(validatorConfig, Object.class);
      if(inputObj instanceof String) {
        input = ImmutableList.of(inputObj.toString());
      }
      else if(inputObj instanceof List) {
        input = new ArrayList<>();
        for(Object inputO : (List<Object>)inputObj) {
          input.add(inputO.toString());
        }
      } else {
        input = new ArrayList<>();
      }
      config = Config.CONFIG.get(validatorConfig, Map.class);
      if(config == null) {
        config = new HashMap<>();
      }
      String validator = Config.VALIDATION.get(validatorConfig, String.class);
      if(validator == null) {
        throw new IllegalStateException("Validation not set.");
      }
      validation= FieldValidations.get(validator);
    }
    else {
      throw new IllegalStateException("Unable to configure field validations");
    }
  }

  public FieldValidation getValidation() {
    return validation;
  }

  public List<String> getInput() {
    return input;
  }

  public Map<String, Object> getConfig() {
    return config;
  }

  /**
   * Runs a validation against Json input data.
   *
   * @param inputData The Json data being validated
   * @param globalConfig The global config feeding the validation
   * @param context The Stellar context of the validation
   * @return true if valid, false otherwise
   */
  public boolean isValid(JSONObject inputData, Map<String, Object> globalConfig, Context context) {
    Map<String, Object> in = inputData;
    if(input != null && !input.isEmpty()) {
      in = new HashMap<>();
      for(String i : input) {
        Object o = inputData.get(i);
        in.put(i,o);
      }
    }
    return validation.isValid(in, config, globalConfig, context);
  }

  /**
   * Reads the validations from the global config and returns a list of them.
   *
   * @param globalConfig The config to read field validations from
   * @return A list of validations
   */
  public static List<FieldValidator> readValidations(Map<String, Object> globalConfig) {
    List<FieldValidator> validators = new ArrayList<>();
    List<Object> validations = (List<Object>) Config.FIELD_VALIDATIONS.get(globalConfig, List.class);
    if(validations != null) {
      for (Object o : validations) {
        FieldValidator f = new FieldValidator(o);
        f.getValidation().initialize(f.getConfig(), globalConfig);
        validators.add(new FieldValidator(o));
      }
    }
    return validators;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    FieldValidator that = (FieldValidator) o;

    if (getValidation() != null ? !getValidation().equals(that.getValidation()) : that.getValidation() != null)
      return false;
    if (getInput() != null ? !getInput().equals(that.getInput()) : that.getInput() != null) return false;
    return getConfig() != null ? getConfig().equals(that.getConfig()) : that.getConfig() == null;

  }

  @Override
  public int hashCode() {
    int result = getValidation() != null ? getValidation().hashCode() : 0;
    result = 31 * result + (getInput() != null ? getInput().hashCode() : 0);
    result = 31 * result + (getConfig() != null ? getConfig().hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "FieldValidator{" +
            "validation=" + validation +
            ", input=" + input +
            ", config=" + config +
            '}';
  }

}
