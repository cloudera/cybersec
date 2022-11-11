package com.cloudera.cyber.hbase;

import org.apache.flink.connector.hbase.util.HBaseConfigurationUtil;
import org.apache.hadoop.conf.Configuration;
import org.junit.Assert;
import org.junit.Test;

public class TestHbaseConfiguration {

    @Test
    public void testHbaseConfiguration() {
        Configuration config = HbaseConfiguration.configureHbase();
        Assert.assertEquals("cybersec-1.vpc.cloudera.com", config.get("hbase.zookeeper.quorum"));
        byte[] serializedConfig = HBaseConfigurationUtil.serializeConfiguration(config);
        Configuration deserializedConfig = HBaseConfigurationUtil.deserializeConfiguration(serializedConfig, null);
        Assert.assertEquals("cybersec-1.vpc.cloudera.com", deserializedConfig.get("hbase.zookeeper.quorum"));
    }

}
