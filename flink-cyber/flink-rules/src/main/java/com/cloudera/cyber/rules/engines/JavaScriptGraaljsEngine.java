package com.cloudera.cyber.rules.engines;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import javax.script.ScriptException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.annotation.VisibleForTesting;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

@Slf4j
public class JavaScriptGraaljsEngine extends JavaScriptEngine {

    private static final Engine ENGINE = Engine.newBuilder().build();
    private static final String LANGUAGE_ID = "js";

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
                    context.eval(LANGUAGE_ID, setupScript + ";" + functionScript);
                }
            } catch (Exception e) {
                isValid = false;
            }

            if (isValid) {
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
        synchronizedEval(script, null);
    }

    @Override
    public Object invokeFunction(String function, Object... args) throws ScriptException, NoSuchMethodException {
        return synchronizedEval(function, (value) -> value.execute(args).as(Object.class));
    }

    private synchronized Object synchronizedEval(String script, Function<Value, Object> postProcessor) {
        Value value = getContext().eval(LANGUAGE_ID, script);
        if (postProcessor != null) {
            return postProcessor.apply(value);
        }
        return value;
    }

    public static JavaScriptGraaljsEngineBuilder builder() {
        return new JavaScriptGraaljsEngineBuilder();
    }

}
