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

package com.cloudera.parserchains.queryservice.config;

import com.cloudera.parserchains.core.*;
import com.cloudera.parserchains.core.catalog.ClassIndexParserCatalog;
import com.cloudera.parserchains.core.catalog.ParserCatalog;
import com.cloudera.parserchains.core.catalog.ParserInfo;
import com.cloudera.parserchains.core.utils.JSONUtils;
import com.cloudera.parserchains.queryservice.common.utils.IDGenerator;
import com.cloudera.parserchains.queryservice.common.utils.UniqueIDGenerator;
import com.cloudera.parserchains.queryservice.model.summary.ObjectMapper;
import com.cloudera.parserchains.queryservice.model.summary.ParserSummary;
import com.cloudera.parserchains.queryservice.model.summary.ParserSummaryMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Paths;

@Configuration
public class MainConfiguration {

  @Bean
  public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
    return JSONUtils.INSTANCE.getMapper();
  }

  @Bean
  public AppProperties appProperties() {
    return new AppProperties();
  }

  @Bean
  public IDGenerator<Long> idGenerator(AppProperties appProperties) {
    return new UniqueIDGenerator(Paths.get(appProperties.getConfigPath()));
  }

  @Bean
  public ParserCatalog parserCatalog() {
    return new ClassIndexParserCatalog();
  }

  @Bean
  public ParserBuilder parserBuilder() {
    return new ReflectiveParserBuilder();
  }

  @Bean
  public ObjectMapper<ParserSummary, ParserInfo> mapper() {
    return new ParserSummaryMapper();
  }

  @Bean
  public ChainRunner chainRunner() {
    return new DefaultChainRunner();
  }

  @Bean
  public ChainBuilder chainBuilder() {
    return new DefaultChainBuilder(parserBuilder(), parserCatalog());
  }
}
