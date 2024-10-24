/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.metron.enrichment.adapters.simplehbase;

import java.io.Serializable;
import org.apache.hadoop.conf.Configuration;
import org.apache.metron.enrichment.utils.EnrichmentUtils;
import org.apache.metron.hbase.HTableProvider;
import org.apache.metron.hbase.TableProvider;


public class SimpleHBaseConfig implements Serializable {
    private String hbaseTable;
    private String hbaseCF;
    private TableProvider provider = new HTableProvider();

    private Configuration hbaseConfig;

    public String getHBaseTable() {
        return hbaseTable;
    }

    public String getHBaseCF() {
        return hbaseCF;
    }

    public TableProvider getProvider() {
        return provider;
    }

    public Configuration getHbaseConfig() {
        return hbaseConfig;
    }

    public SimpleHBaseConfig withProviderImpl(String connectorImpl) {
        provider = EnrichmentUtils.getTableProvider(connectorImpl, new HTableProvider());
        return this;
    }

    public SimpleHBaseConfig withHBaseTable(String hbaseTable) {
        this.hbaseTable = hbaseTable;
        return this;
    }

    public SimpleHBaseConfig withHBaseCF(String cf) {
        this.hbaseCF = cf;
        return this;
    }

    public SimpleHBaseConfig withHbaseConfig(Configuration config) {
        this.hbaseConfig = config;
        return this;
    }
}
