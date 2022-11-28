/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

package com.cloudera.cyber.parser;

import com.cloudera.cyber.Message;
import org.apache.flink.api.common.functions.FilterFunction;

import java.util.ArrayList;
import java.util.List;

public class StreamingEnrichmentsSourceFilter implements FilterFunction<Message> {
    private final ArrayList<String> streamingEnrichmentSources;

    public StreamingEnrichmentsSourceFilter(List<String> streamingEnrichmentSources) {
        this.streamingEnrichmentSources = new ArrayList<>(streamingEnrichmentSources);
    }

    @Override
    public boolean filter(Message message) {
        return streamingEnrichmentSources.contains(message.getSource());
    }
}
