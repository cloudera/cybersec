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

package com.cloudera.cyber.parser;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentsConfig;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.CollectingSink;
import org.apache.flink.test.util.JobTester;
import org.apache.flink.test.util.ManualSource;

import java.security.*;
import java.util.Base64;
import java.util.List;

public abstract class AbstractParserJobTest extends ParserJob {

    protected ManualSource<MessageToParse> source;
    protected CollectingSink<Message> sink = new CollectingSink<>();
    protected CollectingSink<Message> errorSink = new CollectingSink<>();
    protected CollectingSink<EnrichmentCommand> enrichmentCommandSink = new CollectingSink<>();

    @Override
    protected void writeResults(ParameterTool params, DataStream<Message> results) {
        results.addSink(sink);
    }

    @Override
    protected void writeOriginalsResults(ParameterTool params, DataStream<MessageToParse> results) {
    }

    @Override
    protected void writeErrors(ParameterTool params, DataStream<Message> errors) {
        errors.addSink(errorSink);
    }

    @Override
    protected void writeEnrichments(ParameterTool params, DataStream<EnrichmentCommand> streamingEnrichmentResults, List<String> streamingEnrichmentSources, EnrichmentsConfig streamingEnrichmentConfig) {
        streamingEnrichmentResults.addSink(enrichmentCommandSink);
    }

    @Override
    protected DataStream<MessageToParse> createSource(StreamExecutionEnvironment env, ParameterTool params,
            TopicPatternToChainMap topicPatternToChainMap) {
        source = JobTester.createManualSource(env, TypeInformation.of(MessageToParse.class));
        return source.getDataStream();
    }

    protected String getKeyBase64() throws NoSuchProviderException, NoSuchAlgorithmException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA", "SunRsaSign");
        gen.initialize(1024, new SecureRandom());
        KeyPair pair = gen.generateKeyPair();
        return Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
    }

}
