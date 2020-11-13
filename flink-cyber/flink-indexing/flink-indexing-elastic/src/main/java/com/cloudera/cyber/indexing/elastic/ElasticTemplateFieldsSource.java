package com.cloudera.cyber.indexing.elastic;

import com.cloudera.cyber.indexing.CollectionField;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexTemplatesRequest;
import org.elasticsearch.client.indices.GetIndexTemplatesResponse;

import java.time.Instant;
import java.util.Arrays;

import static java.util.stream.Collectors.toList;

@Slf4j
@RequiredArgsConstructor
public class ElasticTemplateFieldsSource extends RichParallelSourceFunction<CollectionField> {

    @NonNull
    private RestHighLevelClient client;
    @NonNull
    private long delay;
    private volatile boolean isRunning = true;

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        HttpHost[] hosts = (HttpHost[]) Arrays.stream(parameters.getString("es.host", "", false).split(","))
                .map(HttpHost::create).toArray();
        this.client = new RestHighLevelClient(RestClient.builder(hosts));
    }

    @Override
    public void run(SourceContext<CollectionField> sourceContext) throws Exception {
        while (isRunning) {
            try {
                GetIndexTemplatesResponse response = client.indices()
                        .getIndexTemplate(new GetIndexTemplatesRequest(), RequestOptions.DEFAULT);
                response.getIndexTemplates().stream()
                        .map(
                                template -> CollectionField.builder()
                                        .key(template.name())
                                        .values(template.mappings().getSourceAsMap().entrySet().stream().map(e -> e.getKey()).collect(toList()))
                                        .build())
                        .forEach(c -> {
                            long now = Instant.now().toEpochMilli();
                            sourceContext.collectWithTimestamp(c, now);
                            sourceContext.emitWatermark(new Watermark(now));
                        });
                Thread.sleep(delay);
            } catch (Exception e) {
                log.error("Solr Collection updater failed", e);
                throw (e);
            }
        }
    }

    @Override
    public void cancel() {
        isRunning = false;
    }
}
