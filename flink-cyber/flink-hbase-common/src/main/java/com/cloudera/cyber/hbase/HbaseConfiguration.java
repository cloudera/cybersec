package com.cloudera.cyber.hbase;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;

import java.io.File;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
public class HbaseConfiguration {
    public static final String HBASE_SITE = "hbase-site.xml";
    public static final String CORE_SITE = "core-site.xml";
    public static final String HDFS_SITE = "hdfs-site.xml";

    public static final String HBASE_CONFIG_NAME = "hbase-config";

    private static final String DEFAULT_CONF_DIR = "/etc/hbase/conf/";

    public static Configuration configureHbase() {
        log.info("HbaseConfiguration configure hbase start");
        Configuration hbaseClientConf = HBaseConfiguration.create();
        Stream.of(HBASE_SITE, CORE_SITE, HDFS_SITE).forEach(confName -> {
            addResource(hbaseClientConf, confName);
        });
        log.info("HbaseConfiguration configure hbase end");
        return hbaseClientConf;
    }

    private static void addResource(Configuration hbaseClientConf, String configXml) {
        if (Objects.nonNull(hbaseClientConf.getResource(configXml))) {
            log.warn("Using local {}", configXml);
        } else {
            String fullPath = HBASE_CONFIG_NAME + configXml;
            if ((new File(fullPath)).exists()) {
                hbaseClientConf.addResource(new Path(fullPath));
                log.info("Adding {} to hbase configuration", fullPath);
            } else {
                log.warn("HBase config directory '{}' not found, cannot load HBase configuration.", DEFAULT_CONF_DIR);
            }
        }
    }
}
