package com.cloudera.cyber.enrichment.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static com.cloudera.cyber.enrichment.rest.RestEnrichmentMethod.GET;


@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(force = true, access = AccessLevel.PUBLIC)
public class RestEnrichmentConfig implements Serializable {

    /** A StringSubstitutor template that puts the message extensions and rest properties into a URL to call */
    @JsonProperty(required=true)
    private String endpointTemplate;

    /** A StringSubstitutor template for the text entity of a POST. Omit for GET. **/
    private String entityTemplate;

    /** HTTP method required by the enrichment rest request. */
    @Builder.Default
    private RestEnrichmentMethod method = GET;

    /** TLS connection configuration keystores and truststores required by server.  Null if client certificates are not required. */
    @Builder.Default
    private TlsConfig tls = null;

    /**
     * Apply rest enrichment to the sources in this list.  If source is ANY, enrichment will be applied to any event that defines the
     * variables used in the url and entity.
     */
    @JsonProperty(required=true)
    private ArrayList<String> sources;

    /**
     * Type of authorization required by the endpoint and the values necessary to construct the Authorization header.
     */
    private EndpointAuthorizationConfig authorization;

    /**
     * Other headers required by the endpoint.
     */
    @Builder.Default
    private HashMap<String, String> headers = new HashMap<>();

    /** Set of properties to be substituted in templates that do not vary by message.  For example, API keys or auth tokens */
    private HashMap<String, String> properties;

    /** Flink async operator timeout in milliseconds */
    @Builder.Default
    private int timeoutMillis = 1000;

    /** Flink async currently open capacity */
    @Builder.Default
    private int capacity = 1000;

    /** Max size of the local results cache */
    @Builder.Default
    private int cacheSize = 10000;

    /** Seconds before a successful cached rest result will be refreshed. */
    @Builder.Default
    private long successCacheExpirationSeconds = TimeUnit.SECONDS.convert(30, TimeUnit.MINUTES);

    /** Seconds before an unsuccessful cached rest result will be refreshed. */
    @Builder.Default
    private long failureCacheExpirationSeconds = TimeUnit.SECONDS.convert(5, TimeUnit.MINUTES);

    /** Prefix for extensions added to the enriched message, e.g. "modelName"*/
    private String prefix;

    /**
     * An expression to determine whether the enrichment should run at all
     */
    private String filterExpression;

    /** Json path to boolean in json result indicating request status.  If null, assume the HTTP status message indicates success or failure. */
    private String successJsonPath;

    /** Json path to map of results.  By default, return the entire json document returned by the endpoint. */
    @Builder.Default
    private String resultsJsonPath = "$";

    public RestRequest createRestEnrichmentRequest() throws Exception {
        RestRequest request = null;
        switch (this.method) {
            case POST :
                request = new PostRestRequest(this);
                break;
            case GET :
                request = new GetRestRequest(this);
                break;
        }
        return request;
    }
}
