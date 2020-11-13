package com.cloudera.cyber.indexing;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import static java.util.stream.Collectors.toList;

@Slf4j
@RequiredArgsConstructor
public class SolrCollectionFieldsSource extends RichParallelSourceFunction<CollectionField> {

    @NonNull private List<String> solrUrls;
    @NonNull private long delay;
    private volatile boolean isRunning = true;

    @Override
    public void run(SourceContext<CollectionField> sourceContext) throws Exception {
        while(isRunning) {
            try {
                loadFieldsFromIndex().forEach(c -> {
                    long now = Instant.now().toEpochMilli();
                    sourceContext.collectWithTimestamp(c, now);
                    sourceContext.emitWatermark(new Watermark(now));
                });
                Thread.sleep(delay);
            } catch(Exception e) {
                log.error("Solr Collection updater failed", e);
                throw(e);
            }
        }
    }

    private List<String> fieldsForCollection(SolrClient solrClient, String collection) {
        SchemaRequest.Fields request = new SchemaRequest.Fields();
        try {
            log.info(String.format("Fetching schema details for %s", collection));
            SchemaResponse.FieldsResponse response = request.process(solrClient, collection);
            return response.getFields().stream().map(m -> m.get("name").toString()).collect(toList());
        } catch (SolrServerException | IOException e) {
            log.error("Problem with Solr Schema inspection", e);
            return new ArrayList<>();
        }
    }

    protected Collection<CollectionField> loadFieldsFromIndex() throws IOException {
        SolrClient solrClient = SolrClientBuilder.builder()
                .solrUrls(solrUrls)
                .build().build();
        try {
            List<String> collections = CollectionAdminRequest.listCollections(solrClient);
            return collections.stream()
                    .map(
                            collection ->
                                    CollectionField.builder()
                                            .key(collection)
                                            .values(fieldsForCollection(solrClient, collection))
                                            .build()
                    )
                    .collect(toList());
        } catch (SolrServerException e) {
            log.error("Problem with Solr Schema inspection", e);
            throw new IOException(e);
        } finally {
            solrClient.close();
        }
    }

    @Override
    public void cancel() {
        isRunning = false;
    }
}
