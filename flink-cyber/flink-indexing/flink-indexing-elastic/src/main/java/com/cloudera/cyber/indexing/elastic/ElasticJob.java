package com.cloudera.cyber.indexing.elastic;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.indexing.IndexEntry;
import com.cloudera.cyber.indexing.SearchIndexJob;
import com.google.common.collect.Streams;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch.RequestIndexer;
import org.apache.flink.streaming.connectors.elasticsearch7.ElasticsearchSink;
import org.apache.http.HttpHost;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexTemplatesRequest;
import org.elasticsearch.client.indices.GetIndexTemplatesResponse;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public abstract class ElasticJob extends SearchIndexJob {
    private static final String PARAMS_ES_HOSTS = "es.hosts";
    private static final String PARAMS_RETRY_TIMEOUT = "es.retry.timeout";
    private RestHighLevelClient client;

    public ElasticJob(ParameterTool params) {
        HttpHost[] hosts = (HttpHost[]) Arrays.stream(params.getRequired("es.host").split(","))
                .map(HttpHost::create).toArray();
        this.client = new RestHighLevelClient(RestClient.builder(hosts));
    }

    @Override
    protected final Map<String, Set<String>> loadFieldsFromIndex(ParameterTool params) throws IOException {
        GetIndexTemplatesResponse response = client.indices()
                .getIndexTemplate(new GetIndexTemplatesRequest(), RequestOptions.DEFAULT);
        return response.getIndexTemplates().stream().collect(toMap(it -> it.name(),
                it -> it.mappings().getSourceAsMap().entrySet().stream().map(e -> e.getKey()).collect(Collectors.toSet())));
    }

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
