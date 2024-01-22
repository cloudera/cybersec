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

package com.cloudera.cyber.enrichment.lookup;

import com.cloudera.cyber.EnrichmentEntry;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.TestUtils;
import com.cloudera.cyber.commands.CommandType;
import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.commands.EnrichmentCommandResponse;
import com.google.common.collect.ImmutableMap;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.test.util.CollectingSink;
import org.apache.flink.test.util.JobTester;
import org.apache.flink.test.util.ManualSource;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.TimeoutException;

import static com.cloudera.cyber.enrichment.ConfigUtils.PARAMS_CONFIG_FILE;
import static com.cloudera.cyber.flink.FlinkUtils.PARAMS_PARALLELISM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;


public class LookupJobTest extends LookupJob {

    CollectingSink<Message> sink = new CollectingSink<>();
    CollectingSink<EnrichmentCommandResponse> queryResults = new CollectingSink<>();
    private ManualSource<Message> source;
    private ManualSource<EnrichmentCommand> enrichmentSource;

    @Test
    public void testEnrichments() throws Exception {
        JobTester.startTest(createPipeline(ParameterTool.fromMap(ImmutableMap.of(
                PARAMS_CONFIG_FILE, "src/test/resources/config.json",
                PARAMS_PARALLELISM, "1"
        ))));
        List<Message> resultMessage = new ArrayList<>();

        // make up some enrichments (three types, multiple fields and multiple entries in some)
        sendEnrichment("ip_whitelist", "10.0.0.1", Collections.singletonMap("whitelist", "true"), 100L);
        sendEnrichment("ip_whitelist", "192.168.0.1", Collections.singletonMap("whitelist", "false"), 100L);
        sendEnrichment("internal_ip", "10.0.0.1", new HashMap<String, String>(2) {{
            put("field1", "1");
            put("field2", "2");
        }}, 100L);
        sendEnrichment("asset", "10.0.0.1", new HashMap<String, String>(2) {{
            put("owner", "mew");
            put("location", "office");
        }}, 100L);

        enrichmentSource.sendWatermark(100L);
        Thread.sleep(3000);

        // make up some enrichable messages, use the message attr to express expected field count after enrichment
        sendMessage("2", Collections.singletonMap("ip_src_addr", "10.0.0.1"), 100);
        sendMessage("4", new HashMap<String, String>(2) {{
            put("ip_src_addr", "10.0.0.1");
            put("ip_dst_addr", "192.168.0.1");
        }}, 150);
        sendMessage("1", Collections.singletonMap("ip_src_addr", "10.0.0.2"), 200);

        source.sendWatermark(200L);

        JobTester.stopTest();

        while (!sink.isEmpty()) {
            resultMessage.add(sink.poll());
        }
        // assert that the sink contains fully enriched entities
        assertThat(resultMessage).hasSize(3).extracting(Message::getMessage).containsExactly("2", "4", "1");
        Message message1 = resultMessage.stream().filter(m -> m.getMessage().equals("1")).findFirst().get();
        Message message2 = resultMessage.stream().filter(m -> m.getMessage().equals("2")).findFirst().get();
        Message message4 = resultMessage.stream().filter(m -> m.getMessage().equals("4")).findFirst().get();
        assertThat(message1.getExtensions()).contains(
                entry("ip_src_addr", "10.0.0.2")
        );
        assertThat(message2.getExtensions()).contains(
                entry("ip_src_addr", "10.0.0.1"),
                entry("ip_src_addr.internal_ip.field1", "1"),
                entry("ip_src_addr.internal_ip.field2", "2"),
                entry("ip_src_addr.ip_whitelist.whitelist", "true"),
                entry("ip_src_addr.asset.location", "office"),
                entry("ip_src_addr.asset.owner", "mew")
        );
        assertThat(message4.getExtensions()).contains(
                entry("ip_src_addr", "10.0.0.1"),
                entry("ip_src_addr.internal_ip.field1", "1"),
                entry("ip_src_addr.internal_ip.field2", "2"),
                entry("ip_src_addr.ip_whitelist.whitelist", "true"),
                entry("ip_src_addr.asset.location", "office"),
                entry("ip_src_addr.asset.owner", "mew"),
                entry("ip_dst_addr", "192.168.0.1"),
                entry("ip_dst_addr.ip_whitelist.whitelist", "false")
                );
    }

    @Override
    protected void writeQueryResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<EnrichmentCommandResponse> sideOutput) {
        sideOutput.addSink(queryResults).name("Enrichment command results").setParallelism(1);
    }

    @Override
    protected void writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<Message> results) {
        results.addSink(sink).name("Enriched messages").setParallelism(1);
    }

    @Override
    public SingleOutputStreamOperator<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        source = JobTester.createManualSource(env, TypeInformation.of(Message.class));
        return source.getDataStream().map(s -> s);
    }

    @Override
    protected DataStream<EnrichmentCommand> createEnrichmentSource(StreamExecutionEnvironment env, ParameterTool params) {
        enrichmentSource = JobTester.createManualSource(env, TypeInformation.of(EnrichmentCommand.class));

        return enrichmentSource.getDataStream()
                .assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor<EnrichmentCommand>(Time.milliseconds(1000)) {
                    @Override
                    public long extractTimestamp(EnrichmentCommand scoringRuleCommand) {
                        return scoringRuleCommand.getPayload().getTs();
                    }
                })
                .setParallelism(1);
    }

    private EnrichmentCommand enrichment(String type, String key, Map<String, String> entries) {
        return EnrichmentCommand.builder()
                .type(CommandType.ADD)
                .headers(Collections.emptyMap())
                .payload(EnrichmentEntry.builder()
                        .type(type)
                        .key(key)
                        .entries(entries)
                        .ts(0)
                        .build()).build();
    }

    private void sendEnrichment(String type, String key, Map<String, String> entries, long ts) throws TimeoutException {
        enrichmentSource.sendRecord(enrichment(type, key, entries), ts);
        // EnrichmentCommandResponse response = queryResults.poll();
        //  assertThat("Command succeed", response.isSuccess());
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

    private void sendMessage(String message, Map<String, String> extensions, long ts) {
        source.sendRecord(message(message, extensions, ts), ts);
    }
}
