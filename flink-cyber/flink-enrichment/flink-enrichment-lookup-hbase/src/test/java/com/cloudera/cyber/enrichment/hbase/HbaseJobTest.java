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

package com.cloudera.cyber.enrichment.hbase;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.TestUtils;
import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.commands.EnrichmentCommandResponse;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentsConfig;
import com.cloudera.cyber.hbase.LookupKey;
import com.google.common.collect.ImmutableMap;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.Utils;
import org.apache.flink.api.java.typeutils.TypeExtractor;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.test.util.CollectingSink;
import org.apache.flink.test.util.JobTester;
import org.apache.flink.test.util.ManualSource;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.platform.commons.support.ReflectionSupport;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.cloudera.cyber.enrichment.ConfigUtils.PARAMS_CONFIG_FILE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@Ignore
public class HbaseJobTest extends HbaseJob implements Serializable {
    private transient ManualSource<Message> source;
    private transient DataStream<Message> dataStreamSource;
    private transient HbaseEnrichmentMapFunction hbaseMapSpy;
    private final transient CollectingSink<EnrichmentCommandResponse> enrichmentResponseSink = new CollectingSink<>();
    private final transient CollectingSink<Message> sink = new CollectingSink<>();

    @Override
    protected DataStream<EnrichmentCommand> createEnrichmentSource(StreamExecutionEnvironment env, ParameterTool params) {
        return JobTester.createManualSource(env, TypeInformation.of(EnrichmentCommand.class)).getDataStream();
    }

    @Override
    public DataStream<EnrichmentCommandResponse> writeEnrichments(StreamExecutionEnvironment env, ParameterTool params,
                                                                  DataStream<EnrichmentCommand> enrichmentSource,
                                                                  EnrichmentsConfig enrichmentsConfig) {
        // usually this would send to hbase
        return JobTester.createManualSource(env, TypeInformation.of(EnrichmentCommandResponse.class)).getDataStream();
    }

    @Override
    protected void writeQueryResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<EnrichmentCommandResponse> enrichmentResults) {
        enrichmentResults.addSink(enrichmentResponseSink);
    }

    @Override
    protected void writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<Message> results) {
        results.addSink(sink);
    }

    @Override
    public DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        source = JobTester.createManualSource(env, TypeInformation.of(Message.class));
        this.dataStreamSource = Mockito.spy(source.getDataStream());
        Mockito.doAnswer((AnswerSerializable<DataStream<Message>>) invocation -> {
            HbaseEnrichmentMapFunction argument = invocation.getArgument(0);
            TypeInformation<Message> unaryOperatorReturnType = TypeExtractor.getUnaryOperatorReturnType(argument, ProcessFunction.class, 0, 1, TypeExtractor.NO_INDEX, dataStreamSource.getType(), Utils.getCallLocationName(), true);
            hbaseMapSpy = Mockito.mock(argument.getClass(),
                    Mockito.withSettings().serializable()
                            .spiedInstance(argument)
                            .defaultAnswer(Mockito.CALLS_REAL_METHODS));
            return dataStreamSource.process(hbaseMapSpy, unaryOperatorReturnType);
        }).when(dataStreamSource).process(Mockito.any());
        return this.dataStreamSource;
    }

    @Test
    public void hbaseEnrichment() throws Exception {
        JobTester.startTest(createPipeline(ParameterTool.fromMap(ImmutableMap.of(
                PARAMS_CONFIG_FILE, "src/test/resources/configs.json",
                PARAMS_ENRICHMENT_CONFIG, "src/test/resources/enrichments_config.json"
        ))));
        List<Message> resultMessage = new ArrayList<>();


        ReflectionSupport.invokeMethod(
                hbaseMapSpy.getClass()
                        .getDeclaredMethod("connectHbase"),
                Mockito.doNothing().when(hbaseMapSpy)
        );
        ReflectionSupport.invokeMethod(
                hbaseMapSpy.getClass()
                        .getDeclaredMethod("fetch", LookupKey.class),
                Mockito.doReturn(ImmutableMap.of("hbaseQualifier", "qualifierValue")).when(hbaseMapSpy),
                Mockito.any(SimpleLookupKey.class)
        );
        ReflectionSupport.invokeMethod(
                hbaseMapSpy.getClass()
                        .getDeclaredMethod("fetch", LookupKey.class),
                Mockito.doReturn(ImmutableMap.of("hbaseQualifier2", "qualifierValue2")).when(hbaseMapSpy),
                Mockito.any(MetronLookupKey.class)
        );

        sendMessage("2", ImmutableMap.of("hostname", "test", "hostname2", "test2"), 200);
        source.sendWatermark(200L);

        JobTester.stopTest();

        while (!sink.isEmpty()) {
            resultMessage.add(sink.poll());
        }
        assertThat(resultMessage).hasSize(1);
        Message message = resultMessage.get(0);
        assertThat(message.getExtensions()).contains(
                entry("hostname", "test"),
                entry("hostname2", "test2"),
                entry("hostname.domain_rep.hbaseQualifier", "qualifierValue"),
                entry("hostname2.domain_rep_2.hbaseQualifier2", "qualifierValue2")
        );
    }

    private Message message(String message, Map<String, String> extensions, long ts) {
        return Message.builder()
                .originalSource(TestUtils.createOriginal())
                .ts(ts)
                .message(message)
                .source("test")
                .extensions(extensions)
                .build();
    }

    private void sendMessage(String strMsg, Map<String, String> extensions, long ts) {
        source.sendRecord(message(strMsg, extensions, ts), ts);
    }

    interface AnswerSerializable<T> extends Answer<T>, Serializable {
        @Override
        T answer(InvocationOnMock invocation) throws Throwable;
    }
}
