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

import lombok.Builder;
import lombok.Data;
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
