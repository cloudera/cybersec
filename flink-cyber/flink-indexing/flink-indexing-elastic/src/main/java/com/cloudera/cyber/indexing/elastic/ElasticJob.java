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

package com.cloudera.cyber.indexing.elastic;

import com.cloudera.cyber.indexing.IndexEntry;
import com.cloudera.cyber.indexing.SearchIndexJob;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch.RequestIndexer;
import org.apache.flink.streaming.connectors.elasticsearch7.ElasticsearchSink;
import org.apache.http.HttpHost;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.RestHighLevelClient;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;

public abstract class ElasticJob extends SearchIndexJob {
    private static final String PARAMS_ES_HOSTS = "es.hosts";
    protected RestHighLevelClient client;

    @Override
    protected final void writeResults(DataStream<IndexEntry> results, ParameterTool params) throws IOException {
        List<HttpHost> httpHosts = Arrays.stream(params.getRequired(PARAMS_ES_HOSTS).split(","))
                .map(HttpHost::create).collect(toList());
        ElasticsearchSink.Builder<IndexEntry> esSinkBuilder = new ElasticsearchSink.Builder<>(
                httpHosts,
                new ElasticsearchSinkFunction<IndexEntry>() {
                    public IndexRequest createIndexRequest(IndexEntry element) {
                        return Requests.indexRequest()
                                .index(element.getIndex())
                                .id(element.getId())
                                .source(element.getFields());

                    }
                    @Override
                    public void process(IndexEntry element, RuntimeContext ctx, RequestIndexer indexer) {
                        indexer.add(createIndexRequest(element));
                    }
                }
        );
        esSinkBuilder.setBulkFlushMaxActions(10000);
        esSinkBuilder.setBulkFlushMaxSizeMb(10);
        esSinkBuilder.setBulkFlushInterval(1000);

        results.addSink(esSinkBuilder.build()).name("Elastic Sink").uid("elastic-sink");
    }
}
