package com.cloudera.cyber.session;

import com.cloudera.cyber.GroupedMessage;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.SignedSourceKey;
import com.cloudera.cyber.sessions.SessionJob;
import com.cloudera.cyber.sha1;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.CollectingSink;
import org.apache.flink.test.util.JobTester;
import org.apache.flink.test.util.ManualSource;
import org.junit.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

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
        sendRecord(Message.newBuilder()
                .setExtensions(createFields(user1))
                .setTs(ts + 0L));
        sendRecord(Message.newBuilder()
                .setExtensions(createFields(user1))
                .setTs(ts + 1000L),true);
        sendRecord(Message.newBuilder()
                .setExtensions(createFields(user1))
                .setTs(ts + 2000L),true);
        sendRecord(Message.newBuilder()
                .setExtensions(createFields(user1))
                .setTs(ts + 9000L),true);

        // session 2
        sendRecord(Message.newBuilder()
                .setExtensions(createFields(user1))
                .setTs(ts + 20000L),true);
        sendRecord(Message.newBuilder()
                .setExtensions(createFields(user1))
                .setTs(ts + 21000L),true);

        source.sendWatermark(50000L);

        JobTester.stopTest();

        List<GroupedMessage> output = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            try{
                output.add(sink.poll(Duration.ofMillis(1000)));
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

    private void sendRecord(Message.Builder builder) {
        sendRecord(builder, false);
    }
    private void sendRecord(Message.Builder builder, boolean watermark) {
        builder.setId(UUID.randomUUID().toString());
        builder.setOriginalSource(SignedSourceKey.newBuilder()
                .setTopic("test")
                .setPartition(0)
                .setOffset(offset++)
                .setSignature(new sha1(new byte[128]))
                .build());
        builder.setMessage("Test Message");
        builder.setSource("test");
        Message message = builder.build();
        source.sendRecord(message, message.getTs());
        if (watermark) source.sendWatermark(message.getTs());
        recordLog.add(message);
    }

    private HashMap<String, Object> createFields(String user) {
        return new HashMap<String, Object>(){{
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
        source = JobTester.createManualSource(env, TypeInformation.of(Message.class));
        return source.getDataStream();
    }


}
