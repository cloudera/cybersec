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
import java.util.Arrays;
import java.util.List;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public abstract class IpGeoJob {
    public static final String PARAM_GEO_FIELDS = "geo.ip_fields";
    public static final String PARAM_GEO_DATABASE_PATH = "geo.database_path";

    public static final String PARAM_ASN_FIELDS = "asn.ip_fields";
    public static final String PARAM_ASN_DATABASE_PATH = "asn.database_path";

    protected StreamExecutionEnvironment createPipeline(ParameterTool params) {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        FlinkUtils.setupEnv(env, params);
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        List<String> ipFields = Arrays.asList(params.getRequired(PARAM_GEO_FIELDS).split(","));
        String geoDatabasePath = params.getRequired(PARAM_GEO_DATABASE_PATH);

        SingleOutputStreamOperator<Message> source = createSource(env, params, ipFields);

        SingleOutputStreamOperator<Message> results = IpGeo.geo(source, ipFields, geoDatabasePath);
        writeResults(params, results);

        return env;
    }

    protected abstract void writeResults(ParameterTool params, DataStream<Message> results);

    protected abstract SingleOutputStreamOperator<Message> createSource(StreamExecutionEnvironment env,
                                                                        ParameterTool params, List<String> ipFields);

}