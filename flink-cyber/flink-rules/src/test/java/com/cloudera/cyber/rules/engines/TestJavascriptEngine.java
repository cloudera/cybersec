package com.cloudera.cyber.rules.engines;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.TestUtils;
import com.google.common.collect.ImmutableMap;
import lombok.val;
import org.junit.Test;

import javax.script.ScriptException;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;

public class TestJavascriptEngine {
    private static final String scoreScript = "return { local: ip_local(message.local), remote: ip_local(message.remote) }";

    @Test
    public void testJavascriptExecution() {
        RuleEngine engine = JavaScriptEngine.builder().script("return { score: parseFloat(message.a) + parseFloat(message.b) }").build();
        engine.open();

        Map<String, Object> results = engine.feed(
                createMessage(Message.builder()
                        .ts(Instant.now().toEpochMilli())
                        .extensions(ImmutableMap.of(
                            "a", "1.0",
                            "b", "2.0"
                        )))
        );

        assertThat("Results are produced", results, hasKey("score"));
        assertThat("Results are correct", Double.valueOf(results.get("score").toString()), equalTo(3.0));
    }

    @Test
    public void testJavascriptEval() throws ScriptException {
        RuleEngine engine = JavaScriptEngine.builder().script(scoreScript).build();
        engine.open();

        engine.eval("function test(a) { log(ip_local(a)) } test('192.168.1.0')");
    }

    @Test
    public void testExtractHostname() {
        String extractHostnameScript = "return { tld: extract_hostname(message.domain, Java.type(\"com.cloudera.cyber.libs.hostnames.ExtractHostname.HostnameFeature\").TLD) }";
        RuleEngine engine = JavaScriptEngine.builder().script(extractHostnameScript).build();
        engine.open();

        Map<String, Object> results = engine.feed(
                createMessage(Message.builder()
                        .ts(Instant.now().toEpochMilli())
                        .extensions(ImmutableMap.of(
                            "domain", "google.com"
                        )))
        );

        assertThat("top level domain correct", results.get("tld"), equalTo("com"));
    }


    @Test
    public void testJavascriptWithUDF() {
        RuleEngine engine = JavaScriptEngine.builder().script(scoreScript).build();
        engine.open();

        Map<String, Object> results = engine.feed(
                createMessage(Message.builder()
                        .ts(Instant.now().toEpochMilli())
                        .extensions(ImmutableMap.of(
                            "local", "192.168.0.1",
                            "remote", "8.8.8.8"
                        )))
        );

        assertThat("Results are produced", results, hasKey("local"));
        assertThat("Remote correct", results.get("remote"), equalTo(false));
        assertThat("Local correct", results.get("local"), equalTo(true));
    }


    @Test
    public void testJavascriptWithMultiArgUDF() {
        RuleEngine engine = JavaScriptEngine.builder().script("return { local: in_subnet(message.local, '192.168.0.1/24') }").build();
        engine.open();

        Map<String, Object> results = engine.feed(
                createMessage(Message.builder()
                        .ts(Instant.now().toEpochMilli())
                        .extensions(ImmutableMap.of(
                            "local", "192.168.0.1",
                            "remote", "8.8.8.8"
                        )))
        );

        assertThat("Results are produced", results, hasKey("local"));
        assertThat("Local correct", results.get("local"), equalTo(true));
    }

    @Test
    public void testJavascriptWithUDFcache() {
        RuleEngine engine = JavaScriptEngine.builder().script("return { local: in_subnet(message.local, '192.168.0.1/24') }").build();
        engine.open();

        for (int i = 0; i < 3; i++) {
            Map<String, Object> results = engine.feed(
                    createMessage(Message.builder()
                            .ts(Instant.now().toEpochMilli())
                            .extensions(ImmutableMap.of(
                                "local", "192.168.0.1"
                     )))
            );
            assertThat(results.get("local"), equalTo(true));
        }
    }

    @Test
    public void testJavascriptWithErrors() {
        RuleEngine invalidEngine = JavaScriptEngine.builder().script("111return { local: in_subnet(message.local, '192.168.0.1/24') }").build();
        assertThat(invalidEngine.validate(), equalTo(false));

        RuleEngine validEngine = JavaScriptEngine.builder().script("return { local: in_subnet(message.local, '192.168.0.1/24') }").build();
        assertThat(validEngine.validate(), equalTo(true));
    }

    private Message createMessage(Message.MessageBuilder builder) {
        return builder
                .message("")
                .source("test")
                .originalSource(TestUtils.source())
                .build();
    }
}
