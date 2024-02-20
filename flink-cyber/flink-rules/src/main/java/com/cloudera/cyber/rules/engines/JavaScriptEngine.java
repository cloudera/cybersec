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
import com.cloudera.cyber.libs.CyberFunctionDefinition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.apache.flink.annotation.VisibleForTesting;

import javax.script.*;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;


@Data
@Slf4j
public class JavaScriptEngine implements RuleEngine {
    private static final String ENGINE_NAME = "graal.js";

    private static final String SCORE_FUNCTION = "score";

    private static final ScriptEngineManager mgr = new ScriptEngineManager();

    @AllArgsConstructor
    @Getter
    private static class ValidatedScriptEngine {
        private final boolean isValid;
        private final ScriptEngine engine;
    }

    private final LazyInitializer<ValidatedScriptEngine> engine = new LazyInitializer<ValidatedScriptEngine>() {

        @Override
        protected ValidatedScriptEngine initialize() {
            return create(getFunctionName(), script);
        }
    };

    @NonNull
    private final String script;
    private final String functionName;

    public JavaScriptEngine(String script) {
        this.script = script;
        this.functionName = SCORE_FUNCTION + "_" +
                Math.abs(this.script.hashCode()) + "_"
                + ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
    }

    private static final String setupScript = CyberFunctionDefinition.findAll().map(JavaScriptEngine::generateJavascript).collect(Collectors.joining(";"));

    private static String generateJavascript(CyberFunctionDefinition functionDefinition) {
        String paramNames = functionDefinition.getParameters().stream().map(Parameter::getName).collect(Collectors.joining(","));
        String functionName = functionDefinition.getFunctionName();
        return functionName + "=function(" + paramNames + "){ return _" + functionName + ".eval(" + paramNames + ");}";
    }

    @Override
    public void open() {

    }

    private static ValidatedScriptEngine create(String functionName, String script) {
        ScriptEngine engine =  mgr.getEngineByName(ENGINE_NAME);
        boolean isValid = true;
        try {
            Bindings globalBindings = engine.getBindings(ScriptContext.GLOBAL_SCOPE);
            CyberFunctionDefinition.findAll().forEach(func -> {
                String udfFunctionName = func.getFunctionName();
                Class<?> implementationClass = func.getImplementationClass();
                try {
                    log.info("Registering {} as {}", implementationClass, udfFunctionName);
                    globalBindings.put("_".concat(udfFunctionName), implementationClass.getDeclaredConstructor().newInstance());
                    log.info("Successfully registered {} as {}", implementationClass, udfFunctionName);
                } catch (Exception e) {
                    log.debug(String.format("Could not register %s as %s", implementationClass, udfFunctionName), e);
                }
            });

            engine.put("log", (Consumer<Object>) (s) -> log.info(s.toString()));

            if (StringUtils.isNotEmpty(script)) {
                String functionScript = "function " + functionName + "(message) { " + script + "}; ";

                log.info(setupScript);
                log.info(functionScript);
                engine.eval(setupScript + ";" + functionScript, engine.getContext());
            }

        } catch (ScriptException e) {
            isValid = false;
        }
        return new ValidatedScriptEngine(isValid, engine);
    }

    @Override
    public void close() {
    }

    @Override
    public boolean validate() {
        try {
            return engine.get().isValid();
        } catch (ConcurrentException e) {
            log.error("Unable to intialize javascript engine", e);
            return false;
        }
    }

    private ScriptEngine getScriptEngine() throws ConcurrentException {
        return engine.get().getEngine();
    }

    @Override
    public Map<String, Object> feed(Message message) {
        try {
            Invocable invokableEngine = (Invocable)(getScriptEngine());
            return (Map<String, Object>) invokableEngine.invokeFunction(getFunctionName(), extractMessage(message));
        } catch (ConcurrentException e) {
            throw new RuntimeException("Unable to initialize javascript rule.", e);
        } catch (ScriptException e) {
            throw new RuntimeException("Javascript Engine function failed", e);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Javascript Engine function build invalid", e);
        } catch (ClassCastException e) {
            throw new RuntimeException("Javascript Engine function returned invalid structure", e);
        }
    }

    @VisibleForTesting
    public void eval(String script) throws ScriptException {
        try {
            getScriptEngine().eval(script);
        } catch (ConcurrentException e) {
            throw new RuntimeException("Unable to initialize javascript rule.", e);
        }
    }

    @Override
    public Object invokeFunction(String function, Object... args) throws ScriptException, NoSuchMethodException {
        try {
            return ((Invocable) getScriptEngine()).invokeFunction(function, args);
        } catch (ConcurrentException e) {
            throw new RuntimeException("Unable to initialize javascript rule.", e);
        }
    }

    private HashMap<String, Object> extractMessage(Message message) {
        HashMap<String, Object> flat = new HashMap<>(message.getExtensions());
        flat.put("ts", message.getTs());
        flat.put("id", message.getId());
        flat.put("source", message.getSource());
        return flat;
    }

    public static JavascriptEngineBuilder builder() {
        return new JavascriptEngineBuilder();
    }
}
