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

package com.cloudera.cyber.stellar;

import com.cloudera.cyber.parser.MessageToParse;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.metron.common.message.metadata.MetadataUtil;
import org.apache.metron.common.message.metadata.RawMessage;
import org.apache.metron.common.message.metadata.RawMessageStrategy;
import org.apache.metron.common.utils.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum MetronRawDataExtractor {

    INSTANCE;
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static final int KEY_INDEX = 1;

    /**
     * Extract the raw message given the strategy, the tuple and the metadata configs.
     *
     * @param strategy       The {@link RawMessageStrategy} to use for extraction
     * @param messageToParse The original message and its topic metadata
     * @param readMetadata   True if read metadata, false otherwise
     * @param config         The config to use during extraction
     * @return The resulting {@link RawMessage}
     */
    public RawMessage getRawMessage(RawMessageStrategy strategy, MessageToParse messageToParse, boolean readMetadata,
                                    Map<String, Object> config) {
        Map<String, Object> metadata = new HashMap<>();
        if (readMetadata) {
            String prefix = MetadataUtil.INSTANCE.getMetadataPrefix(config);
            metadata = extractMetadata(prefix, messageToParse);
        }
        return strategy.get(metadata, messageToParse.getOriginalBytes(), readMetadata, config);
    }

    /**
     * Default extraction of metadata.  This handles looking in the normal places for metadata
     * <ul>
     *   <li>The kafka key</li>
     *   <li>The tuple fields outside of the value (e.g. the topic)</li>
     * </ul>
     *
     * <p>In addition to extracting the metadata into a map,
     * it applies the appropriate prefix (as configured in the rawMessageStrategyConfig).
     *
     * @param prefix         The prefix of the metadata keys
     * @param messageToParse The message read from kafka and the topic information
     * @return A map containing the metadata
     */
    public Map<String, Object> extractMetadata(String prefix, MessageToParse messageToParse) {
        Map<String, Object> metadata = new HashMap<>();

        addMetaData(prefix, metadata, "topic", messageToParse.getTopic());
        byte[] keyObj = messageToParse.getKey();
        String keyStr = null;
        try {
            keyStr = keyObj == null ? null : new String(keyObj, StandardCharsets.UTF_8);
            if (!StringUtils.isEmpty(keyStr)) {
                Map<String, Object> rawMetadata = JSONUtils.INSTANCE.load(keyStr, JSONUtils.MAP_SUPPLIER);
                for (Map.Entry<String, Object> kv : rawMetadata.entrySet()) {
                    metadata.put(MetadataUtil.INSTANCE.prefixKey(prefix, kv.getKey()), kv.getValue());
                }

            }
        } catch (IOException e) {
            String reason = "Unable to parse metadata; expected JSON Map: " + (keyStr == null ? "NON-STRING!" : keyStr);
            LOG.error(reason, e);
            throw new IllegalStateException(reason, e);
        }
        return metadata;
    }

    private void addMetaData(String prefix, Map<String, Object> metadata, String envMetadataFieldName,
                             Object envMetadataFieldValue) {
        if (!StringUtils.isEmpty(envMetadataFieldName) && envMetadataFieldValue != null) {
            metadata.put(MetadataUtil.INSTANCE.prefixKey(prefix, envMetadataFieldName), envMetadataFieldValue);
        }
    }


}
