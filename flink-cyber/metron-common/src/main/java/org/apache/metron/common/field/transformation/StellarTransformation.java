/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.metron.common.field.transformation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.metron.stellar.common.CachingStellarProcessor;
import org.apache.metron.stellar.common.StellarProcessor;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.MapVariableResolver;
import org.apache.metron.stellar.dsl.StellarFunctions;

public class StellarTransformation implements FieldTransformation {
    @Override
    public Map<String, Object> map(Map<String, Object> input,
                                   List<String> outputField,
                                   LinkedHashMap<String, Object> fieldMappingConfig,
                                   Context context,
                                   Map<String, Object>... sensorConfig) {
        Map<String, Object> ret = new HashMap<>();
        Map<String, Object> intermediateVariables = new HashMap<>();
        Set<String> outputs = new HashSet<>(outputField);
        MapVariableResolver resolver = new MapVariableResolver(ret, intermediateVariables, input);
        resolver.add(sensorConfig);
        StellarProcessor processor = new CachingStellarProcessor();
        for (Map.Entry<String, Object> kv : fieldMappingConfig.entrySet()) {
            String objField = kv.getKey();
            Object transformObj = kv.getValue();
            if (transformObj != null) {
                try {
                    Object o = processor.parse(transformObj.toString(), resolver, StellarFunctions.FUNCTION_RESOLVER(),
                          context);
                    if (o != null) {
                        if (outputs.contains(objField)) {
                            ret.put(objField, o);
                        } else {
                            intermediateVariables.put(objField, o);
                        }
                    } else {
                        if (outputs.contains(objField)) {
                            ret.put(objField, o);
                        }
                        if (o != null) {
                            intermediateVariables.put(objField, o);
                        } else {
                            // remove here, in case there are other statements
                            intermediateVariables.remove(objField);
                        }
                    }
                } catch (Exception ex) {
                    throw new IllegalStateException("Unable to process transformation: " + transformObj
                                                    + " for " + objField + " because " + ex.getMessage(), ex);
                }
            }
        }
        return ret;
    }
}
