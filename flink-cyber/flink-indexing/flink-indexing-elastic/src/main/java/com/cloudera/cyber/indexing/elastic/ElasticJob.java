package com.cloudera.cyber.indexing.elastic;

import com.cloudera.cyber.Message;
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

    private static HttpHost urlToHost(String in) {
        try {
            URL url = new URL(in);
            return new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
        } catch (MalformedURLException e) {
            return null;
        }
    }


    public ElasticJob(ParameterTool params) {
        client = new RestHighLevelClient(RestClient.builder(new HttpHost(params.getRequired("es.host"), 9200, "http")));
    }

    @Override
    protected Map<String, Set<String>> loadFieldsFromIndex(ParameterTool params) throws IOException {
        GetIndexTemplatesResponse response = client.indices()
                .getIndexTemplate(new GetIndexTemplatesRequest(), RequestOptions.DEFAULT);
        return response.getIndexTemplates().stream().collect(toMap(it -> it.name(),
                it -> it.mappings().getSourceAsMap().entrySet().stream().map(e -> e.getKey()).collect(Collectors.toSet())));
    }

    @Override
    protected void writeResults(DataStream<Message> results, ParameterTool params) throws IOException {
        List<HttpHost> httpHosts = Arrays.stream(params.getRequired(PARAMS_ES_HOSTS).split(","))
                .map(ElasticJob::urlToHost).collect(toList());
        Map<String, Set<String>> fields = loadFieldsFromIndex(params);
        ElasticsearchSink.Builder<Message> esSinkBuilder = new ElasticsearchSink.Builder<Message>(
                httpHosts,
                new ElasticsearchSinkFunction<Message>() {
                    public IndexRequest createIndexRequest(Message element) {
                        Stream<Map.Entry<String, ?>> source = Streams.concat(
                                element.getExtensions().entrySet().stream()
                                        .filter(e ->fields.get(element.getSource()).contains(e.getKey())),
                                Collections.singletonMap("ts", element.getTs()).entrySet().stream()
                        );

                        return Requests.indexRequest()
                                .index(element.getSource())
                                .id(element.getId())
                                .source(source.collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));

                    }

                    @Override
                    public void process(Message element, RuntimeContext ctx, RequestIndexer indexer) {
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
