package com.cloudera.cyber.rules.engines;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.TestUtils;
import org.junit.Test;

import javax.script.ScriptException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Thread.sleep;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;

public class TestJavascriptEngine {
    private static final String scoreScript = "return { local: ip_local(message.local), remote: ip_local(message.remote) }";

    @Test
    public void testJavascriptExecution() throws InterruptedException {
        RuleEngine engine = JavaScriptEngine.builder().script("return { score: parseFloat(message.a) + parseFloat(message.b) }").build();
        engine.open();

        sleep(100);

        Map<String, Object> results = engine.feed(
                createMessage(Message.builder()
                        .ts(Instant.now().toEpochMilli())
                        .extensions(new HashMap<String, String>() {{
                            put("a", "1.0");
                            put("b", "2.0");
                        }}))
        );

        assertThat("Results are produced", results, hasKey("score"));
        assertThat("Results are correct", Double.valueOf(results.get("score").toString()), equalTo(3.0));
    }

    @Test
    public void testJavascriptEval() throws ScriptException, InterruptedException {
        RuleEngine engine = JavaScriptEngine.builder().script(scoreScript).build();
        engine.open();

        sleep(100);

        engine.eval("function test(a) { log(ip_local(a)) } test('192.168.1.0')");
    }

    @Test
    public void testJavascriptWithUDF() throws InterruptedException {
        RuleEngine engine = JavaScriptEngine.builder().script(scoreScript).build();
        engine.open();

        sleep(100);

        Map<String, Object> results = engine.feed(
                createMessage(Message.builder()
                        .ts(Instant.now().toEpochMilli())
                        .extensions(new HashMap<String, String>() {{
                            put("local", "192.168.0.1");
                            put("remote", "8.8.8.8");
                        }}))
        );

        assertThat("Results are produced", results, hasKey("local"));
        assertThat("Remote correct", results.get("remote"), equalTo(false));
        assertThat("Local correct", results.get("local"), equalTo(true));
    }


    @Test
    public void testJavascriptWithMultiArgUDF() throws InterruptedException {
        RuleEngine engine = JavaScriptEngine.builder().script("return { local: in_subnet(message.local, '192.168.0.1/24') }").build();
        engine.open();

        sleep(100);

        Map<String, Object> results = engine.feed(
                createMessage(Message.builder()
                        .ts(Instant.now().toEpochMilli())
                        .extensions(new HashMap<String, String>() {{
                            put("local", "192.168.0.1");
                            put("remote", "8.8.8.8");
                        }}))
        );

        assertThat("Results are produced", results, hasKey("local"));
        assertThat("Local correct", results.get("local"), equalTo(true));
    }

    @Test
    public void testJavascriptWithUDFcache() throws InterruptedException {
        RuleEngine engine = JavaScriptEngine.builder().script("return { local: in_subnet(message.local, '192.168.0.1/24') }").build();
        engine.open();

        sleep(100);

        for (int i = 0; i < 3; i++) {
            Map<String, Object> results = engine.feed(
                    createMessage(Message.builder()
                            .ts(Instant.now().toEpochMilli())
                            .extensions(new HashMap<String, String>() {{
                                put("local", "192.168.0.1");
                            }}))
            );
        }
    }

    private Message createMessage(Message.MessageBuilder builder) {
        return builder
                .message("")
                .source("test")
                .originalSource(TestUtils.source())
                .build();
    }
}
