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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.formats.avro.typeutils.AvroTypeInfo;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toList;

@Slf4j
@RequiredArgsConstructor
public class SolrCollectionFieldsSource extends RichParallelSourceFunction<CollectionField> implements ResultTypeQueryable<CollectionField> {
    @NonNull
    private List<String> solrUrls;
    @NonNull
    private long delay;
    private volatile boolean isRunning = true;

    @Override
    public void run(SourceContext<CollectionField> sourceContext) throws Exception {
        loadFieldsFromIndex().forEach(collector(sourceContext));
        while (isRunning) {
            try {
                loadFieldsFromIndex().forEach(collector(sourceContext));
            } catch (Exception e) {
                log.error("Solr Collection updater failed", e);
                throw (e);
            }
            Thread.sleep(delay);
        }
    }

    private Consumer<? super CollectionField> collector(SourceContext<CollectionField> sourceContext) {
        return c -> {
            long now = Instant.now().toEpochMilli();
            sourceContext.collectWithTimestamp(c, now);
        };
    }

    private List<String> fieldsForCollection(SolrClient solrClient, String collection) {
        SchemaRequest.Fields request = new SchemaRequest.Fields();
        try {
            SchemaResponse.FieldsResponse response = request.process(solrClient, collection);
            log.info("Fetching schema details for {}. Response: {}", collection, response);
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

    @Override
    public TypeInformation<CollectionField> getProducedType() {
        return new AvroTypeInfo(CollectionField.class);
    }
}
