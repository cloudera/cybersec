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

package com.cloudera.cyber.session;

import com.cloudera.cyber.GroupedMessage;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.SignedSourceKey;
import com.cloudera.cyber.sessions.SessionJob;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.formats.avro.typeutils.AvroTypeInfo;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.CollectingSink;
import org.apache.flink.test.util.JobTester;
import org.apache.flink.test.util.ManualSource;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@Ignore("Test broken by serialization nonsense")
public class TestSessionizer extends SessionJob {
    private ManualSource<Message> source;
    private CollectingSink<GroupedMessage> sink = new CollectingSink<>();
    private List<Message> recordLog = new ArrayList<>();
    private int offset = 0;

    @Test
    public void testSingleUserTwoSessions() throws Exception {
        long ts = 0;

        JobTester.startTest(createPipeline(ParameterTool.fromMap(new HashMap<String, String>() {{
            put(PARAM_SESSION_KEY,"user");
            put(PARAM_SESSION_TIMEOUT,"7000");
            put(PARAM_SESSION_LIMIT,"20000");
        }})));

        // send a bunch of session results

        String user1 = UUID.randomUUID().toString();
        String user2 = UUID.randomUUID().toString();

        // session 1
        sendRecord(Message.builder()
                .extensions(createFields(user1))
                .ts(ts + 0L));
        sendRecord(Message.builder()
                .extensions(createFields(user1))
                .ts(ts + 1000L),true);
        sendRecord(Message.builder()
                .extensions(createFields(user1))
                .ts(ts + 2000L),true);
        sendRecord(Message.builder()
                .extensions(createFields(user1))
                .ts(ts + 9000L),true);

        // session 2
        sendRecord(Message.builder()
                .extensions(createFields(user1))
                .ts(ts + 20000L),true);
        sendRecord(Message.builder()
                .extensions(createFields(user1))
                .ts(ts + 21000L),true);

        source.sendWatermark(50000L);

        JobTester.stopTest();

        List<GroupedMessage> output = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            try{
                output.add(sink.poll());
            } catch(TimeoutException e){
                break;
            }
        }
        output.forEach(this::checkResult);

        assertThat("Output has all sessions", output, hasSize(2));
    }

    private void checkResult(GroupedMessage groupedMessage) {
        assertThat("There are some messages in the group", groupedMessage.getMessages().size(), greaterThan(0));
        assertThat("Group has an id", groupedMessage.getId(), notNullValue());
    }

    private void sendRecord(Message.MessageBuilder builder) {
        sendRecord(builder, false);
    }
    private void sendRecord(Message.MessageBuilder builder, boolean watermark) {
        builder.originalSource(SignedSourceKey.builder()
                .topic("test")
                .partition(0)
                .offset(offset++)
                .signature(new byte[128])
                .build())
                .message("Test Message")
                .source("test");
        Message message = builder.build();
        source.sendRecord(message, message.getTs());
        if (watermark) source.sendWatermark(message.getTs());
        recordLog.add(message);
    }

    private HashMap<String, String> createFields(String user) {
        return new HashMap<String, String>(){{
                put("user", user);
            }};
    }

    @Override
    protected void writeResults(ParameterTool params, SingleOutputStreamOperator<GroupedMessage> results) {
        results.addSink(sink);
    }

    @Override
    protected DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool
            params, List<String> sessionKey, Long sessionTimeout) {
        source = JobTester.createManualSource(env, AvroTypeInfo.of(Message.class));
        return source.getDataStream();
    }


}
