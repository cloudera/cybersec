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

package com.cloudera.cyber.indexing;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.EventTimeAndCountTrigger;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;

@Slf4j
public abstract class SolrJob extends SearchIndexJob {

    private static final String PARAMS_INDEX_WINDOW_MAX_MS = "index.time.ms";
    private static final long DEFAULT_INDEX_WINDOW_MAX_MS = 10000;
    private static final long DEFAULT_MAX_EVENTS = 10000;

    protected void writeResults(DataStream<IndexEntry> results, ParameterTool params) {
        results.keyBy(IndexEntry::getIndex).timeWindow(Time.milliseconds(params.getLong(PARAMS_INDEX_WINDOW_MAX_MS, DEFAULT_INDEX_WINDOW_MAX_MS)))
                .trigger(EventTimeAndCountTrigger.of(DEFAULT_MAX_EVENTS))
                .apply(new SolrIndexer(params))
                .name("Solr Indexer")
                .uid("Solr Indexer");
    }

    protected abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);
}
