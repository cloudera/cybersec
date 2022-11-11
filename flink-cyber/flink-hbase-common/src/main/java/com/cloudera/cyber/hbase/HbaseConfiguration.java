package com.cloudera.cyber.hbase;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.connector.hbase.util.HBaseConfigurationUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;

import java.util.List;

@Slf4j
public class HbaseConfiguration {
    private static final List<String> HBASE_CONFIG_FILES = Lists.newArrayList("core-site.xml", "hdfs-site.xml", "hbase-site.xml" );
    public static final String HBASE_CONFIG_NAME = "hbase-config";

    public static Configuration configureHbase() {
        log.info("HbaseConfiguration configure hbase start");
        Configuration hbaseClientConf = HBaseConfiguration.create();
        // jars in the classpath add a default hbase configuration
        // if the local files are just the default config try to load other paths
        if (!"localhost".equals(hbaseClientConf.get("hbase.zookeeper.quorum"))) {
            log.warn("Using local configs");
        } else {
           hbaseClientConf = HBaseConfigurationUtil.getHBaseConfiguration();
        }
        log.info("HbaseConfiguration configure hbase end");
        return hbaseClientConf;
    }

}
