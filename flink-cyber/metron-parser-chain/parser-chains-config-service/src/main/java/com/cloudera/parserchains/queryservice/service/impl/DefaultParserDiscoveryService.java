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

package com.cloudera.parserchains.queryservice.service.impl;

import com.cloudera.parserchains.core.Parser;
import com.cloudera.parserchains.core.catalog.Configurable;
import com.cloudera.parserchains.core.catalog.Parameter;
import com.cloudera.parserchains.core.catalog.ParserCatalog;
import com.cloudera.parserchains.core.catalog.ParserInfo;
import com.cloudera.parserchains.core.model.define.ParserID;
import com.cloudera.parserchains.core.model.define.ParserName;
import com.cloudera.parserchains.queryservice.model.describe.ConfigParamDescriptor;
import com.cloudera.parserchains.queryservice.model.describe.ParserDescriptor;
import com.cloudera.parserchains.queryservice.model.summary.ObjectMapper;
import com.cloudera.parserchains.queryservice.model.summary.ParserSummary;
import com.cloudera.parserchains.queryservice.service.ParserDiscoveryService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.cloudera.parserchains.core.utils.AnnotationUtils.getAnnotatedMethods;
import static com.cloudera.parserchains.core.utils.AnnotationUtils.getAnnotatedParameters;

@Service
public class DefaultParserDiscoveryService implements ParserDiscoveryService {
  static final String DEFAULT_PATH_ROOT = "config";
  static final String PATH_DELIMITER = ".";

  @Autowired
  private ParserCatalog catalog;

  @Autowired
  private ObjectMapper<ParserSummary, ParserInfo> mapper;

  @Autowired
  public DefaultParserDiscoveryService(ParserCatalog catalog, ObjectMapper<ParserSummary, ParserInfo> mapper) {
    this.catalog = catalog;
    this.mapper = mapper;
  }

  @Override
  public List<ParserSummary> findAll() throws IOException {
    return catalog.getParsers()
            .stream()
            .map(info -> mapper.reform(info))
            .collect(Collectors.toList());
  }

  @Override
  public ParserDescriptor describe(ParserID name) throws IOException {
    return describeAll().get(name);
  }

  @Override
  public Map<ParserID, ParserDescriptor> describeAll() throws IOException {
    return catalog.getParsers()
            .stream()
            .collect(Collectors.toMap(
                    info -> ParserID.of(info.getParserClass()),
                    info -> describeParser(info)));
  }

  private ParserDescriptor describeParser(ParserInfo parserInfo) {
    List<ConfigParamDescriptor> descriptors = describeParameters(parserInfo.getParserClass());

    // sort by name for consistency
    Comparator<ConfigParamDescriptor> compareByName = Comparator.comparing(ConfigParamDescriptor::getName);
    Collections.sort(descriptors, compareByName);

    ParserID id = ParserID.of(parserInfo.getParserClass());
    ParserName name = ParserName.of(parserInfo.getName());
    return new ParserDescriptor()
            .setParserID(id)
            .setParserName(name)
            .setConfigurations(descriptors);
  }

  private List<ConfigParamDescriptor> describeParameters(Class<? extends Parser> parserClass) {
    List<ConfigParamDescriptor> results = new ArrayList<>();
    Set<Map.Entry<Configurable, Method>> annotatedMethods = getAnnotatedMethods(parserClass).entrySet();
    for(Map.Entry<Configurable, Method> entry: annotatedMethods) {
      Configurable configurable = entry.getKey();
      Method method = entry.getValue();

      List<Parameter> parameters = getAnnotatedParameters(method);
      if(parameters.size() == 0) {
        // there are no parameter annotations
        ConfigParamDescriptor descriptor = describeByAnnotation(configurable, Optional.empty());
        results.add(descriptor);

      } else {
        // there are parameter annotations
        for (Parameter parameter : parameters) {
          ConfigParamDescriptor descriptor = describeByAnnotation(configurable, Optional.of(parameter));
          results.add(descriptor);
        }
      }
    }
    return results;
  }

  private ConfigParamDescriptor describeByAnnotation(Configurable configurable, Optional<Parameter> parameter) {
    /*
     * If multiple=true, the front-end expects values to be contained within an array. if
     * multiple=false, the value should NOT be wrapped in an array; just a single map.
     * Currently, the backend always wraps values, even single values, in arrays.
     *
     * Having the backend adhere to what the front-end expects will take some additional
     * work. As a work-around all configurations are marked as accepting multiple values,
     * even those that do not.  The consequence of this is that all fields will show the blue,
     * plus icon to add a field.
     */
    ConfigParamDescriptor paramDescriptor = new ConfigParamDescriptor()
            .setPath(DEFAULT_PATH_ROOT + PATH_DELIMITER + configurable.key())
            .setMultiple(true)
            .setMultipleValues(configurable.multipleValues());
    if (parameter.isPresent()) {
      // use the parameter-level annotation
      Parameter p = parameter.get();
      paramDescriptor.setName(p.key())
              .setLabel(p.label())
              .setDescription(p.description())
              .setRequired(p.required())
              .setOutputName(p.isOutputName())
              .setType(p.widgetType());
      if (StringUtils.isNotBlank(p.defaultValue())) {
        paramDescriptor.addDefaultValue(p.key(), p.defaultValue());
      }

    } else {
      // use the method-level annotation
      paramDescriptor.setName(configurable.key())
              .setLabel(configurable.label())
              .setDescription(configurable.description())
              .setRequired(configurable.required())
              .setOutputName(configurable.isOutputName())
              .setType(configurable.widgetType());
      if (StringUtils.isNotBlank(configurable.defaultValue())) {
        paramDescriptor.addDefaultValue(configurable.key(), configurable.defaultValue());
      }
    }

    return paramDescriptor;
  }
}
