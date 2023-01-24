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

package com.cloudera.cyber.enrichment.geocode;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import java.io.IOException;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.Arrays;
import java.util.List;

public abstract class IpRegionCidrJob {
    public static final String PARAM_CIDR_IP_FIELDS = "cidr.ip_fields";
    public static final String PARAM_CIDR_CONFIG_PATH = "cidr.config_file_path";

    protected StreamExecutionEnvironment createPipeline(ParameterTool params) throws IOException {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        FlinkUtils.setupEnv(env, params);
        List<String> ipFields = Arrays.asList(params.getRequired(PARAM_CIDR_IP_FIELDS).split(","));
        String cidrConfigPath = params.getRequired(PARAM_CIDR_CONFIG_PATH);

        SingleOutputStreamOperator<Message> source = createSource(env, params, ipFields);

        SingleOutputStreamOperator<Message> results = IpRegionCidr.cidr(source, ipFields, cidrConfigPath);
        writeResults(params, results);

        return env;
    }

    protected abstract void writeResults(ParameterTool params, DataStream<Message> results);

    protected abstract SingleOutputStreamOperator<Message> createSource(StreamExecutionEnvironment env, ParameterTool params, List<String> ipFields);

}