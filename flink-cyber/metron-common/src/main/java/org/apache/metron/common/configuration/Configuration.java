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

package org.apache.metron.common.configuration;

import java.nio.file.Path;

/**
 * Allows users to have a {@link Configurations} that can use a ZooKeeper curator or a file during
 * updates of the global config.
 */
public class Configuration extends Configurations {

    private final Path configFileRoot;

    /**
     * Constructor for interacting with a file.
     *
     * @param configFileRoot The config file path to use
     */
    public Configuration(Path configFileRoot) {

        this.configFileRoot = configFileRoot;
    }

    /**
     * If there's a ZooKeeper client available, use that for updating configs, otherwise
     * update global configs from a file.
     *
     * @throws Exception If there's an issue updating the config
     */
    public void update() throws Exception {
        updateGlobalConfig(ConfigurationsUtils.readGlobalConfigFromFile(configFileRoot.toAbsolutePath().toString()));
    }
}
