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
package org.apache.metron.common.zookeeper.configurations;

import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.common.configuration.ConfigurationType;
import org.apache.metron.common.configuration.IndexingConfigurations;

import java.io.IOException;
import java.util.function.Supplier;

public class IndexingUpdater extends ConfigurationsUpdater<IndexingConfigurations> {
  public IndexingUpdater(Reloadable reloadable, Supplier<IndexingConfigurations> configSupplier) {
    super(reloadable, configSupplier);
  }

  @Override
  public Class<IndexingConfigurations> getConfigurationClass() {
    return IndexingConfigurations.class;
  }

  @Override
  public void forceUpdate(CuratorFramework client) {
  }

  @Override
  public IndexingConfigurations defaultConfigurations() {
    return new IndexingConfigurations();
  }

  @Override
  public ConfigurationType getType() {
    return ConfigurationType.INDEXING;
  }

  @Override
  public void delete(String name) {
    getConfigurations().delete(name);
  }

  @Override
  public void update(String name, byte[] data) throws IOException {
    getConfigurations().updateSensorIndexingConfig(name, data);
  }

}
