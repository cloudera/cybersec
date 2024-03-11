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

package com.cloudera.cyber.rules.engines;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.TestUtils;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.script.ScriptException;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;

public class TestJavascriptEngine {
    private static final String scoreScript = "return { local: ip_local(message.local), remote: ip_local(message.remote) }";

    public static Stream<Arguments> jsBuilders() {
        return Stream.of(Arguments.of(JavaScriptNashornEngine.builder()),
                Arguments.of(JavaScriptGraaljsEngine.builder()));
    }

    @ParameterizedTest
    @MethodSource("jsBuilders")
    public void testJavascriptExecution(RuleEngineBuilder<? extends JavaScriptEngine> jsBuilder) {
        RuleEngine engine = jsBuilder.script("return { score: parseFloat(message.a) + parseFloat(message.b) }").build();
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

    @ParameterizedTest
    @MethodSource("jsBuilders")
    public void testJavascriptEval(RuleEngineBuilder<? extends JavaScriptEngine> jsBuilder) throws ScriptException {
        RuleEngine engine = jsBuilder.script(scoreScript).build();
        engine.open();

        engine.eval("function test(a) { log(ip_local(a)) } test('192.168.1.0')");
    }

    @ParameterizedTest
    @MethodSource("jsBuilders")
    public void testExtractHostname(RuleEngineBuilder<? extends JavaScriptEngine> jsBuilder) {
        String extractHostnameScript = "return { tld: extract_hostname(message.domain, Java.type(\"com.cloudera.cyber.libs.hostnames.ExtractHostname.HostnameFeature\").TLD) }";
        RuleEngine engine = jsBuilder.script(extractHostnameScript).build();
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

    @ParameterizedTest
    @MethodSource("jsBuilders")
    public void testJavascriptWithUDF(RuleEngineBuilder<? extends JavaScriptEngine> jsBuilder) {
        RuleEngine engine = jsBuilder.script(scoreScript).build();
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

    @ParameterizedTest
    @MethodSource("jsBuilders")
    public void testJavascriptWithMultiArgUDF(RuleEngineBuilder<? extends JavaScriptEngine> jsBuilder) {
        RuleEngine engine = jsBuilder.script("return { local: in_subnet(message.local, '192.168.0.1/24') }").build();
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

    @ParameterizedTest
    @MethodSource("jsBuilders")
    public void testJavascriptWithUDFcache(RuleEngineBuilder<? extends JavaScriptEngine> jsBuilder) {
        RuleEngine engine = jsBuilder.script("return { local: in_subnet(message.local, '192.168.0.1/24') }").build();
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

    @ParameterizedTest
    @MethodSource("jsBuilders")
    public void testJavascriptWithErrors(RuleEngineBuilder<? extends JavaScriptEngine> jsBuilder) {
        RuleEngine invalidEngine = jsBuilder.script("111return { local: in_subnet(message.local, '192.168.0.1/24') }").build();
        assertThat(invalidEngine.validate(), equalTo(false));

        RuleEngine validEngine = jsBuilder.script("return { local: in_subnet(message.local, '192.168.0.1/24') }").build();
        assertThat(validEngine.validate(), equalTo(true));
    }

    //FIXME resolve GraalJS 1.0 -> 1 conversion problem
//    @ParameterizedTest
//    @MethodSource("jsBuilders")
    public void testProperNumberConversion(RuleEngineBuilder<? extends JavaScriptEngine> jsBuilder) {
        RuleEngine engine = jsBuilder.script("return { testInt: 1, testDouble: 1.0 }").build();
        Map<String, Object> result = engine.feed(createMessage(Message.builder()
                .ts(Instant.now().toEpochMilli())
                .extensions(ImmutableMap.of())));

        assertThat(result.get("testInt"), equalTo(1));
        assertThat(result.get("testDouble"), equalTo(1.0));
    }

    private Message createMessage(Message.MessageBuilder builder) {
        return builder
                .message("")
                .source("test")
                .originalSource(TestUtils.source())
                .build();
    }
}
