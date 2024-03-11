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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.apache.flink.annotation.VisibleForTesting;

import javax.script.*;


@Slf4j
@Deprecated
public class JavaScriptNashornEngine extends JavaScriptEngine {
    private static final String ENGINE_NAME = "javascript";

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

    public JavaScriptNashornEngine(String script) {
        super(script);
    }

    private static ValidatedScriptEngine create(String functionName, String script) {
        ScriptEngine engine = mgr.getEngineByName(ENGINE_NAME);
        boolean isValid = true;
        try {
            Bindings globalBindings = engine.getBindings(ScriptContext.GLOBAL_SCOPE);
            initBindings(globalBindings::put);

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

    public static JavaScriptNashornEngineBuilder builder() {
        return new JavaScriptNashornEngineBuilder();
    }
}
