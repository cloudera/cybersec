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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

/**
 * Application properties should be in lowercase!
 */
public class AppProperties {

  private enum Options implements ConfigOption<String, Environment> {
    CONFIG_PATH("."),
    PIPELINES_PATH("../../.."),
    SAMPLE_FOLDER_PATH("../samples/"),
    INDEX_PATH("../../index/conf/mapping-config.json");

    @Override
    public String get(Environment source) {
      return source.getProperty(optionKey, defaultValue);
    }

    private String optionKey;
    private String defaultValue;

    Options(String defaultValue) {
      this.optionKey = normalizeProperty(name());
      this.defaultValue = defaultValue;
    }

    private String normalizeProperty(String name) {
      return name.replace('_', '.').toLowerCase();
    }
  }

  @Autowired
  private Environment environment;

  public AppProperties() {
  }

  @Autowired
  AppProperties(Environment environment) {
    this.environment = environment;
  }

  public String getConfigPath() {
    return Options.CONFIG_PATH.get(environment);
  }

  public String getPipelinesPath() {
    return Options.PIPELINES_PATH.get(environment);
  }

  public String getIndexPath() {
    return Options.INDEX_PATH.get(environment);
  }

  public String getSampleFolderPath() {
    return Options.SAMPLE_FOLDER_PATH.get(environment);
  }
}
