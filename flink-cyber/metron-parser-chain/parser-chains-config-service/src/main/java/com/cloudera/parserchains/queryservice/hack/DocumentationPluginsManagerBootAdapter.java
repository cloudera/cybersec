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

package com.cloudera.parserchains.queryservice.hack;

/*
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.plugin.core.PluginRegistry;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.DefaultsProviderPlugin;
import springfox.documentation.spi.service.ResourceGroupingStrategy;
import springfox.documentation.spi.service.contexts.DocumentationContextBuilder;
import springfox.documentation.spring.web.SpringGroupingStrategy;
import springfox.documentation.spring.web.plugins.DefaultConfiguration;
import springfox.documentation.spring.web.plugins.DocumentationPluginsManager;

 */

public class DocumentationPluginsManagerBootAdapter {

}
    /*extends DocumentationPluginsManager {

  @Autowired
  @Qualifier("resourceGroupingStrategyRegistry")
  private PluginRegistry<ResourceGroupingStrategy, DocumentationType> resourceGroupingStrategies;

  @Autowired
  @Qualifier("defaultsProviderPluginRegistry")
  private PluginRegistry<DefaultsProviderPlugin, DocumentationType> defaultsProviders;

  @Override
  public ResourceGroupingStrategy resourceGroupingStrategy(DocumentationType documentationType) {
    return resourceGroupingStrategies
        .getPluginOrDefaultFor(documentationType, new SpringGroupingStrategy());
  }

  @Override
  public DocumentationContextBuilder createContextBuilder(DocumentationType documentationType,
      DefaultConfiguration defaultConfiguration) {
    return defaultsProviders.getPluginOrDefaultFor(documentationType, defaultConfiguration)
        .create(documentationType)
        .withResourceGroupingStrategy(resourceGroupingStrategy(documentationType));
  }
}

     */

