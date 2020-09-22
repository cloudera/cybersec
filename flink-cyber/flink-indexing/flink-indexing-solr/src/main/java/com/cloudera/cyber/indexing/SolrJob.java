package com.cloudera.cyber.indexing;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.EventTimeAndCountTrigger;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;

import java.io.IOException;
import java.util.*;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@Slf4j
public abstract class SolrJob extends SearchIndexJob {

    private static final String PARAMS_INDEX_WINDOW_MAX_MS = "index.time.ms";
    private static final long DEFAULT_INDEX_WINDOW_MAX_MS = 10000;
    private static final long DEFAULT_MAX_EVENTS = 10000;

    @Override
    protected Map<String, Set<String>> loadFieldsFromIndex(ParameterTool params) throws IOException {
        SolrClient solrClient = SolrClientBuilder.builder()
                .solrUrls(Arrays.asList(params.getRequired("solr.urls").split(",")))
                .build().build();
        try {
            List<String> collections = CollectionAdminRequest.listCollections(solrClient);
            return collections.stream().collect(toMap(
                    collection -> collection,
                    collection -> {
                        SchemaRequest.Fields request = new SchemaRequest.Fields();
                        try {
                            SchemaResponse.FieldsResponse response = request.process(solrClient, collection);
                            return response.getFields().stream().map(m -> m.get("name").toString()).collect(toSet());
                        } catch (SolrServerException | IOException e) {
                            log.error("Problem with Solr Schema inspection", e);
                            return new HashSet<String>();
                        }
                    }));
        } catch (SolrServerException e) {
            throw new IOException(e);
        }
    }

    protected void writeResults(DataStream<IndexEntry> results, ParameterTool params) {
        results.keyBy(IndexEntry::getIndex).timeWindow(Time.milliseconds(params.getLong(PARAMS_INDEX_WINDOW_MAX_MS, DEFAULT_INDEX_WINDOW_MAX_MS)))
                .trigger(EventTimeAndCountTrigger.of(DEFAULT_MAX_EVENTS))
                .apply(new SolrIndexer(params))
                .name("Solr Indexer")
                .uid("Solr Indexer");
    }

    protected abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);

}
