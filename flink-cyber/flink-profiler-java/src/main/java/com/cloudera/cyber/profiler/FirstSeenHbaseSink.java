package com.cloudera.cyber.profiler;

import com.cloudera.cyber.hbase.AbstractHbaseSinkFunction;
import org.apache.flink.api.java.utils.ParameterTool;


public class FirstSeenHbaseSink extends AbstractHbaseSinkFunction<ProfileMessage> {

    public FirstSeenHbaseSink(String hTableName, String columnFamily, ProfileGroupConfig profileGroupConfig, ParameterTool params) {
        super(hTableName, new FirstSeenHbaseMutationConverter(hTableName, columnFamily, profileGroupConfig), params, "numFirstSeen");
    }

}
