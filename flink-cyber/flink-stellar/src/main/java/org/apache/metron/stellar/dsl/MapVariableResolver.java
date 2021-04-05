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

package org.apache.metron.stellar.dsl;


import org.apache.metron.stellar.common.utils.ConcatMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapVariableResolver implements VariableResolver {


  List<Map> variableMappings = new ArrayList<>();

  public MapVariableResolver(Map variableMappingOne, Map... variableMapping) {
    if (variableMappingOne != null) {
      variableMappings.add(variableMappingOne);
    }
    add(variableMapping);
  }

  public void add(Map... ms) {
    if (ms != null) {
      for (Map m : ms) {
        if (m != null) {
          this.variableMappings.add(m);
        }
      }
    }
  }

  @Override
  public Object resolve(String variable) {
    if(variable != null && variable.equals(VariableResolver.ALL_FIELDS)) {
      return new ConcatMap(variableMappings);
    }

    for (Map variableMapping : variableMappings) {
      Object o = variableMapping.get(variable);
      if (o != null) {
        return o;
      }
    }
    return null;
  }

  @Override
  public boolean exists(String variable) {
    return true;
  }
}
