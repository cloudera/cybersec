package com.cloudera.cyber.rules.engines;

import com.cloudera.cyber.Message;
import org.junit.Test;

import javax.script.ScriptException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;

public class TestJavascriptEngine {
    private static final String scoreScript = "return { local: ip_local(message.local), remote: ip_local(message.remote) }";

    @Test
    public void testJavascriptExecution() {
        RuleEngine engine = JavaScriptEngine.builder().script("return { score: message.a + message.b }").build();
        engine.open();

        Map<String, Object> results = engine.feed(
                Message.builder().fields(new HashMap<String, Object>() {{
                    put("a", 1.0);
                    put("b", 2.0);
                }}).build()
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
    public void testJavascriptWithUDF() {
        RuleEngine engine = JavaScriptEngine.builder().script(scoreScript).build();
        engine.open();

        Map<String, Object> results = engine.feed(
                Message.builder().fields(new HashMap<String, Object>() {{
                    put("local", "192.168.0.1");
                    put("remote", "8.8.8.8");
                }}).build()
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
                Message.builder().fields(new HashMap<String, Object>() {{
                    put("local", "192.168.0.1");
                    put("remote", "8.8.8.8");
                }}).build()
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
                    Message.builder().fields(new HashMap<String, Object>() {{
                        put("local", "192.168.0.1");
                    }}).build()
            );
        }
    }
}
