/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.common.configuration;

import org.apache.commons.io.FilenameUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.apache.metron.common.configuration.ConfigurationType.*;

/**
 * A Utility class for managing various configs, including global configs, sensor specific configs,
 * and profiler configs.
 */
public class ConfigurationsUtils {
    protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Sets up Stellar statically with a connection to ZooKeeper and optionally, global configuration
     * to be used.
     *
     * @param client       The ZK client to be used
     * @param globalConfig Optional global configuration to be used
     */
    public static void setupStellarStatically(CuratorFramework client, Optional<String> globalConfig) {
    /*
      In order to validate stellar functions, the function resolver must be initialized.  Otherwise,
      those utilities that require validation cannot validate the stellar expressions necessarily.
    */
        Context.Builder builder = new Context.Builder()
                .with(Context.Capabilities.ZOOKEEPER_CLIENT, () -> client);

        if (globalConfig.isPresent()) {
            builder = builder
                    .with(Context.Capabilities.GLOBAL_CONFIG, () -> GLOBAL.deserialize(globalConfig.get()))
                    .with(Context.Capabilities.STELLAR_CONFIG, () -> GLOBAL.deserialize(globalConfig.get()));
        } else {
            builder = builder
                    .with(Context.Capabilities.STELLAR_CONFIG, () -> new HashMap<>());
        }
        Context stellarContext = builder.build();
        StellarFunctions.FUNCTION_RESOLVER().initialize(stellarContext);
    }

    /**
     * Reads global configs from a file on local disk.
     *
     * @param rootPath root FS location to read configs from
     * @return map of file names to the contents of that file as a byte array
     * @throws IOException If there's an issue reading the configs
     */
    public static byte[] readGlobalConfigFromFile(String rootPath) throws IOException {
        byte[] globalConfig = new byte[0];
        File configPath = new File(rootPath, GLOBAL.getTypeName() + ".json");
        if (configPath.exists()) {
            globalConfig = Files.readAllBytes(configPath.toPath());
        }
        return globalConfig;
    }

    /**
     * Reads sensor parser configs from a file on local disk.
     *
     * @param rootPath root FS location to read configs from
     * @return map of file names to the contents of that file as a byte array
     * @throws IOException If there's an issue reading the configs
     */
    public static Map<String, byte[]> readSensorParserConfigsFromFile(String rootPath) throws IOException {
        return readSensorConfigsFromFile(rootPath, PARSER, Optional.empty());
    }

    /**
     * Reads sensor enrichment configs from a file on local disk.
     *
     * @param rootPath root FS location to read configs from
     * @return map of file names to the contents of that file as a byte array
     * @throws IOException If there's an issue reading the configs
     */
    public static Map<String, byte[]> readSensorEnrichmentConfigsFromFile(String rootPath) throws IOException {
        return readSensorConfigsFromFile(rootPath, ENRICHMENT, Optional.empty());
    }

    /**
     * Reads sensor indexing configs from a file on local disk.
     *
     * @param rootPath root FS location to read configs from
     * @return map of file names to the contents of that file as a byte array
     * @throws IOException If there's an issue reading the configs
     */
    public static Map<String, byte[]> readSensorIndexingConfigsFromFile(String rootPath) throws IOException {
        return readSensorConfigsFromFile(rootPath, INDEXING, Optional.empty());
    }

    /**
     * Reads sensor configs from a file on local disk.
     *
     * @param rootPath   root FS location to read configs from
     * @param configType e.g. GLOBAL, PARSER, ENRICHMENT, etc.
     * @return map of file names to the contents of that file as a byte array
     * @throws IOException If there's an issue reading the configs
     */
    public static Map<String, byte[]> readSensorConfigsFromFile(String rootPath, ConfigurationType configType) throws IOException {
        return readSensorConfigsFromFile(rootPath, configType, Optional.empty());
    }

    /**
     * Will read configs from local disk at the specified rootPath. Will read all configs for a given
     * configuration type. If an optional specific config name is also provided, it will only read
     * configs for that configuration type and name combo. e.g. PARSER, bro
     *
     * @param rootPath   root FS location to read configs from
     * @param configType e.g. GLOBAL, PARSER, ENRICHMENT, etc.
     * @param configName a specific config, for instance a sensor name like bro, yaf, snort, etc.
     * @return map of file names to the contents of that file as a byte array
     * @throws IOException If there's an issue reading the configs
     */
    public static Map<String, byte[]> readSensorConfigsFromFile(String rootPath,
                                                                ConfigurationType configType, Optional<String> configName) throws IOException {
        Map<String, byte[]> sensorConfigs = new HashMap<>();
        File configPath = new File(rootPath, configType.getDirectory());
        if (configPath.exists() && configPath.isDirectory()) {
            File[] children = configPath.listFiles();
            if (!configName.isPresent()) {
                for (File file : children) {
                    sensorConfigs.put(FilenameUtils.removeExtension(file.getName()),
                            Files.readAllBytes(file.toPath()));
                }
            } else {
                for (File file : children) {
                    if (FilenameUtils.removeExtension(file.getName()).equals(configName.get())) {
                        sensorConfigs.put(FilenameUtils.removeExtension(file.getName()),
                                Files.readAllBytes(file.toPath()));
                    }
                }
                if (sensorConfigs.isEmpty()) {
                    throw new RuntimeException("Unable to find configuration for " + configName.get());
                }
            }
        }
        return sensorConfigs;
    }

    /**
     * Gets the field name from a map of the global config. Returns a default if the global config
     * itself is null or the key isn't found.
     *
     * @param globalConfig     Map of the global config
     * @param globalConfigKey  The key too retrieve from the map
     * @param defaultFieldName The default to use if config is null or key not found
     * @return The config value or the default if config is null or key not found
     */
    public static String getFieldName(Map<String, Object> globalConfig, String globalConfigKey, String defaultFieldName) {
        if (globalConfig == null) {
            return defaultFieldName;
        }
        return (String) globalConfig.getOrDefault(globalConfigKey, defaultFieldName);
    }
}

