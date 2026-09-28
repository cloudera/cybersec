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
import com.cloudera.cyber.enrichment.Enrichment;
import com.cloudera.cyber.enrichment.EnrichmentConfiguration;
import com.cloudera.cyber.enrichment.geocode.database.IpAsnEnrichment;
import com.cloudera.cyber.enrichment.geocode.database.IpCompanyEnrichment;
import com.cloudera.cyber.enrichment.geocode.database.IpGeoEnrichment;
import com.cloudera.cyber.flink.FlinkUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class IpGeoJob {
    public static final String PARAMS_ENABLE_GEO = "geo.enabled";
    public static final String PARAMS_ENABLE_ASN = "asn.enabled";
    public static final String PARAMS_ENABLE_COMPANY = "company.enabled";

    public static final String PARAM_GEO_FIELDS = "geo.ip_fields";
    public static final String PARAM_GEO_DATABASE_PATH = "geo.database_path";

    public static final String PARAM_ASN_FIELDS = "asn.ip_fields";
    public static final String PARAM_ASN_DATABASE_PATH = "asn.database_path";

    public static final String PARAM_COMPANY_FIELDS = "company.ip_fields";
    public static final String PARAM_COMPANY_DATABASE_PATH = "company.database_path";

    protected StreamExecutionEnvironment createPipeline(ParameterTool params) {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        FlinkUtils.setupEnv(env, params);

        SingleOutputStreamOperator<Message> source = createSource(env, params);
        writeResults(params, enrich(params, source));

        return env;
    }

    protected abstract void writeResults(ParameterTool params, DataStream<Message> results);

    protected abstract SingleOutputStreamOperator<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);

    public static SingleOutputStreamOperator<Message> enrich(ParameterTool params, SingleOutputStreamOperator<Message> messages) {
        List<String> geoFields = EnrichmentConfiguration.getFieldsForGeoEnrichment(params, PARAMS_ENABLE_GEO, PARAM_GEO_FIELDS);
        SingleOutputStreamOperator<Message> geoEnriched = !geoFields.isEmpty() ?
                IpGeo.geo(messages,
                        geoFields,
                        params.getRequired(PARAM_GEO_DATABASE_PATH)) : messages;

        List<String> asnFields = EnrichmentConfiguration.getFieldsForGeoEnrichment(params, PARAMS_ENABLE_ASN, PARAM_ASN_FIELDS);
        SingleOutputStreamOperator<Message> asnEnriched = !asnFields.isEmpty() ?
                IpGeo.asn(geoEnriched,
                        asnFields,
                        params.getRequired(PARAM_ASN_DATABASE_PATH)) : geoEnriched;

        List<String> companyFields = EnrichmentConfiguration.getFieldsForGeoEnrichment(params, PARAMS_ENABLE_COMPANY, PARAM_COMPANY_FIELDS);
        return !companyFields.isEmpty() ?
                IpGeo.company(asnEnriched,
                        companyFields,
                        params.getRequired(PARAM_COMPANY_DATABASE_PATH)) : asnEnriched;
    }

}