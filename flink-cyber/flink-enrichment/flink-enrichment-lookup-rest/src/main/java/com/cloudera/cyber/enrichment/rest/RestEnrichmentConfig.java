package com.cloudera.cyber.enrichment.rest;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RestEnrichmentConfig {
    /**
     * A template that puts the message fields into a URL to call
     */
    private String endpointTemplate;

    /**
     * Source to be enriched like this
     */
    private String source;

    // add auth and ssl stuff


    // async stuff
    /**
     * Async timeout in milliseconds
     */
    @Builder.Default
    private int timeout = 1000;

    /**
     * Async currently open capacity
     */
    @Builder.Default
    private int capacity = 1000;


    /**
     * Max size of the local cache
     */
    @Builder.Default
    private int cacheSize = 10000;

}
