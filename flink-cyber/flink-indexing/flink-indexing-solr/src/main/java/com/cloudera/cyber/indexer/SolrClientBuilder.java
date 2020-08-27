package com.cloudera.cyber.indexer;

import lombok.Data;
import lombok.Builder;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.Krb5HttpClientBuilder;
import org.apache.solr.client.solrj.impl.SolrHttpClientBuilder;

import java.util.List;

@Data
@Builder
public class SolrClientBuilder {
    private List<String> solrUrls;
    private String trustStorePath;
    private String trustStorePassword;

    public SolrClient build() {
        if (trustStorePath != null) {
            System.setProperty("javax.net.ssl.trustStore", trustStorePath);
        }

        if (trustStorePassword != null) {
            System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
        }

        Krb5HttpClientBuilder krbBuild = new Krb5HttpClientBuilder();
        SolrHttpClientBuilder kb = krbBuild.getBuilder();
        HttpClientUtil.setHttpClientBuilder(kb);
        return new CloudSolrClient.Builder(solrUrls).build();
    }
}
