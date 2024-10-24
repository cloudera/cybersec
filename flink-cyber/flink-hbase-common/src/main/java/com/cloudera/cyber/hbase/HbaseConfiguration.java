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

package com.cloudera.cyber.hbase;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.connector.hbase.util.HBaseConfigurationUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;

@Slf4j
public class HbaseConfiguration {
    private static final List<String> HBASE_CONFIG_FILES =
          Lists.newArrayList("core-site.xml", "hdfs-site.xml", "hbase-site.xml");
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
