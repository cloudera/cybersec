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

package org.apache.metron.enrichment.adapters.threatintel;

import java.io.Serializable;
import org.apache.hadoop.conf.Configuration;
import org.apache.metron.enrichment.utils.EnrichmentUtils;
import org.apache.metron.hbase.HTableProvider;
import org.apache.metron.hbase.TableProvider;

public class ThreatIntelConfig implements Serializable {
    public static final long MS_IN_HOUR = 10000 * 60 * 60;
    private String hbaseTable;
    private String hbaseCF;

    private Configuration hbaseConfig;

    private double falsePositiveRate = 0.03;
    private int expectedInsertions = 100000;
    private String trackerHBaseTable;
    private String trackerHBaseCF;
    private long millisecondsBetweenPersists = 2 * MS_IN_HOUR;
    private TableProvider provider = new HTableProvider();

    public String getHBaseTable() {
        return hbaseTable;
    }

    public int getExpectedInsertions() {
        return expectedInsertions;
    }

    public Configuration getHbaseConfig() {
        return hbaseConfig;
    }

    public double getFalsePositiveRate() {
        return falsePositiveRate;
    }

    public String getTrackerHBaseTable() {
        return trackerHBaseTable;
    }

    public String getTrackerHBaseCF() {
        return trackerHBaseCF;
    }

    public long getMillisecondsBetweenPersists() {
        return millisecondsBetweenPersists;
    }

    public String getHBaseCF() {
        return hbaseCF;
    }

    public TableProvider getProvider() {
        return provider;
    }

    public ThreatIntelConfig withProviderImpl(String connectorImpl) {
        provider = EnrichmentUtils.getTableProvider(connectorImpl, new HTableProvider());
        return this;
    }

    public ThreatIntelConfig withTrackerHBaseTable(String hbaseTable) {
        this.trackerHBaseTable = hbaseTable;
        return this;
    }

    public ThreatIntelConfig withTrackerHBaseCF(String cf) {
        this.trackerHBaseCF = cf;
        return this;
    }

    public ThreatIntelConfig withHBaseTable(String hbaseTable) {
        this.hbaseTable = hbaseTable;
        return this;
    }

    public ThreatIntelConfig withHBaseCF(String cf) {
        this.hbaseCF = cf;
        return this;
    }

    public ThreatIntelConfig withFalsePositiveRate(double falsePositiveRate) {
        this.falsePositiveRate = falsePositiveRate;
        return this;
    }

    public ThreatIntelConfig withExpectedInsertions(int expectedInsertions) {
        this.expectedInsertions = expectedInsertions;
        return this;
    }

    public ThreatIntelConfig withMillisecondsBetweenPersists(long millisecondsBetweenPersists) {
        this.millisecondsBetweenPersists = millisecondsBetweenPersists;
        return this;
    }

    public ThreatIntelConfig withHbaseConfig(Configuration hbaseConfig) {
        this.hbaseConfig = hbaseConfig;
        return this;

    }
}
