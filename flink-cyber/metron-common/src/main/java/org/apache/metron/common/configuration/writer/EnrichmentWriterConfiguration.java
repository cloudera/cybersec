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

package org.apache.metron.common.configuration.writer;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.metron.common.configuration.EnrichmentConfigurations;

/**
 * Writer configuration for the enrichment topology. Batch size and batch timeout are a couple
 * primary values of interest that are used for configuring the enrichment writer.
 */
public class EnrichmentWriterConfiguration implements WriterConfiguration {

    private final Optional<EnrichmentConfigurations> config;

    public EnrichmentWriterConfiguration(EnrichmentConfigurations config) {
        this.config = Optional.ofNullable(config);
    }

    /**
     * Batch size for writing.
     *
     * @param sensorName n/a
     * @return batch size in # messages
     */
    @Override
    public int getBatchSize(String sensorName) {
        return config.orElse(new EnrichmentConfigurations()).getBatchSize();
    }

    /**
     * Timeout for this writer.
     *
     * @param sensorName n/a
     * @return timeout in ms
     */
    @Override
    public int getBatchTimeout(String sensorName) {
        return config.orElse(new EnrichmentConfigurations()).getBatchTimeout();
    }

    /**
     * Timeout for this writer.
     *
     * @return single item list with this writer's timeout
     */
    @Override
    public List<Integer> getAllConfiguredTimeouts() {
        return Collections.singletonList(getBatchTimeout(null));
    }

    /**
     * n/a for enrichment.
     *
     * @param sensorName n/a
     * @return null
     */
    @Override
    public String getIndex(String sensorName) {
        return null;
    }

    /**
     * Always enabled in enrichment.
     *
     * @param sensorName n/a
     * @return true
     */
    @Override
    public boolean isEnabled(String sensorName) {
        return true;
    }

    @Override
    public Map<String, Object> getSensorConfig(String sensorName) {
        return config.orElse(new EnrichmentConfigurations()).getSensorEnrichmentConfig(sensorName)
                     .getConfiguration();
    }

    @Override
    public Map<String, Object> getGlobalConfig() {
        return config.orElse(new EnrichmentConfigurations()).getGlobalConfig();
    }

    @Override
    public boolean isDefault(String sensorName) {
        return false;
    }

    @Override
    public String getFieldNameConverter(String sensorName) {
        // not applicable
        return null;
    }
}
