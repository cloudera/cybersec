package com.cloudera.cyber.rules.engines;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.annotation.VisibleForTesting;
import org.graalvm.polyglot.*;

import javax.script.ScriptException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class JavaScriptGraaljsEngine extends JavaScriptEngine {

    private static final Engine ENGINE = Engine.newBuilder().build();
    private static final String LANGUAGE_ID = "js";
    private static final Map<String, Source> cachedScripts = new HashMap<>();

    private final AtomicBoolean isOpen = new AtomicBoolean(false);
    private ValidatedContext validatedContext;

    @AllArgsConstructor
    @Getter
    private static class ValidatedContext {
        private final boolean isValid;
        private final Context context;
    }

    public JavaScriptGraaljsEngine(String script) {
        super(script);
        validatedContext = new ValidatedContext(false, null);
    }

    private static Source getCachedScript(String script) {
        if (StringUtils.isEmpty(script)) {
            return null;
        }
        cachedScripts.computeIfAbsent(script, (s) -> Source.create(LANGUAGE_ID, s));
        return cachedScripts.get(script);
    }

    @Override
    public void open() {
        if (!isOpen.get()) {
            Context context = Context.newBuilder()
                    .engine(ENGINE)
                    .allowHostAccess(HostAccess.ALL)
                    .allowHostClassLookup(className -> true)
                    .build();

            Value bindings = context.getBindings(LANGUAGE_ID);
            initBindings(bindings::putMember);

            boolean isValid = true;

            try {
                if (StringUtils.isNotEmpty(script)) {
                    String functionScript = "function " + functionName + "(message) { " + script + "}; ";

                    log.info(setupScript);
                    log.info(functionScript);
                    Source cachedScript = getCachedScript(setupScript + ";" + functionScript);
                    if (cachedScript != null) {
                        context.eval(cachedScript);
                    } else {
                        isValid = false;
                    }
                }
            } catch (Exception e) {
                isValid = false;
            }

            if (isValid){
                context.initialize(LANGUAGE_ID);
            }

            validatedContext = new ValidatedContext(isValid, context);
            isOpen.set(true);
        }
    }

    @Override
    public void close() {
        Context context = getContext();
        if (context != null) {
            context.close();
        }
    }

    @Override
    public boolean validate() {
        open();
        return validatedContext.isValid();
    }

    private Context getContext() {
        open();
        return validatedContext.getContext();
    }

    @Override
    @VisibleForTesting
    public void eval(String script) throws ScriptException {
        Source cachedScript = getCachedScript(script);
        if (cachedScript == null){
            throw new ScriptException("Wasn't able to compile the provided script!");
        }
        getContext().eval(cachedScript);
    }

    @Override
    public Object invokeFunction(String function, Object... args) throws ScriptException, NoSuchMethodException {
        Source source = getCachedScript(function);
        return getContext().eval(source).execute(args).as(Object.class);
    }

    public static JavaScriptGraaljsEngineBuilder builder() {
        return new JavaScriptGraaljsEngineBuilder();
    }

}
