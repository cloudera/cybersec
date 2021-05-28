package com.cloudera.cyber.hbase;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;

import java.io.File;

@Slf4j
public class HbaseConfiguration {
    public static org.apache.hadoop.conf.Configuration configureHbase() {
        org.apache.hadoop.conf.Configuration hbaseClientConf = HBaseConfiguration.create();
        String hbaseConfDir = "/etc/hbase/conf";
        if ((new File(hbaseConfDir)).exists()) {
            String coreSite = hbaseConfDir + "/core-site.xml";
            String hdfsSite = hbaseConfDir + "/hdfs-site.xml";
            String hbaseSite = hbaseConfDir + "/hbase-site.xml";
            if ((new File(coreSite)).exists()) {
                hbaseClientConf.addResource(new Path(coreSite));
                log.info("Adding " + coreSite + " to hbase configuration");
            }

            if ((new File(hdfsSite)).exists()) {
                hbaseClientConf.addResource(new Path(hdfsSite));
                log.info("Adding " + hdfsSite + " to hbase configuration");
            }

            if ((new File(hbaseSite)).exists()) {
                hbaseClientConf.addResource(new Path(hbaseSite));
                log.info("Adding " + hbaseSite + " to hbase configuration");
            }
        } else {
            log.warn("HBase config directory '{}' not found, cannot load HBase configuration.", hbaseConfDir);
        }

        return hbaseClientConf;
    }
}
