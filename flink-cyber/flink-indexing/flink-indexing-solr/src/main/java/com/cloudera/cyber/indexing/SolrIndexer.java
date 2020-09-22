package com.cloudera.cyber.indexing;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.windowing.RichWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.groupingBy;

@Slf4j
public class SolrIndexer extends RichWindowFunction<IndexEntry, UpdateResponse, String, TimeWindow> {

    public static final String SOLR_URLS_KEY = "solr.urls";
    public static final String SOLR_COLLECTION_KEY = "solr.collection";

    public static final String SOLR_SSL_LOCATION_KEY = "solr.ssl.truststore.location";
    public static final String SOLR_SSL_PWD_KEY = "solr.ssl.truststore.password";

    private final List<String> solrUrls;
    private final String solrCollection;
    private final String trustStorePath;
    private final String trustStorePassword;

    private transient SolrClient solrClient;

    public SolrIndexer(ParameterTool params) {
        solrUrls = Lists.newArrayList(params.getRequired(SOLR_URLS_KEY).split(","));
        solrCollection = params.getRequired(SOLR_COLLECTION_KEY);
        trustStorePath = params.get(SOLR_SSL_LOCATION_KEY);
        trustStorePassword = params.get(SOLR_SSL_PWD_KEY);
    }

    @Override
    public void apply(String k, TimeWindow w, Iterable<IndexEntry> logs, Collector<UpdateResponse> output) throws Exception {
        Map<String, List<IndexEntry>> collect = StreamSupport.stream(logs.spliterator(), false).collect(groupingBy(IndexEntry::getIndex));
        // TODO - account for all the errors, not just the last
        AtomicReference<Exception> lastError = new AtomicReference<>();
        collect.forEach((collection, entries) -> {
            try {
                output.collect(solrClient.add(collection, mapToSolrDocuments(entries)));
            } catch (SolrServerException | IOException e) {
                log.error("Solr Exception", e);
                lastError.set(e);
            }
        });
        if (lastError.get() != null) {
            throw(lastError.get());
        }
    }

    private List<SolrInputDocument> mapToSolrDocuments(Iterable<IndexEntry> logs) {
        List<SolrInputDocument> docs = new ArrayList<>();
        for (IndexEntry entry : logs) {
            SolrInputDocument doc = new SolrInputDocument();
            entry.getFields().forEach(doc::addField);
            doc.addField("timestamp", entry.getTimestamp());
            doc.addField("id", entry.getId());
            docs.add(doc);
        }
        return docs;
    }

    @Override
    public void open(Configuration config) {
        solrClient = SolrClientBuilder.builder()
                .solrUrls(solrUrls)
                .build().build();
    }

    @Override
    public void close() throws IOException {
        if (solrClient != null) {
            solrClient.close();
            solrClient = null;
        }
    }
}
