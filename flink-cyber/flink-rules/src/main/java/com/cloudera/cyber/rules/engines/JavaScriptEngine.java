package com.cloudera.cyber.rules.engines;

import com.cloudera.cyber.CyberFunction;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.libs.CyberFunctionUtils;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.flink.annotation.VisibleForTesting;

import javax.script.*;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@RequiredArgsConstructor
@Log
public class JavaScriptEngine implements RuleEngine {
    private static final String ENGINE_NAME = "javascript";

    private static final String SCORE_FUNCTION = "score";

    private final ScriptEngineManager mgr = new ScriptEngineManager();
    private final ScriptEngine engine = mgr.getEngineByName(ENGINE_NAME);

    @NonNull
    private String script;
    private String $functionName = "";

    @Override
    public void open() {
        try {
            String setupScript = CyberFunctionUtils.findAll()
                    .map(e -> {
                        String functionName = e.getAnnotation(CyberFunction.class).value();
                        try {
                            return ImmutablePair.of(
                                    functionName,
                                    ImmutablePair.of(e.newInstance(),
                                            Stream.of(e.getDeclaredMethods()).filter(m -> m.getName().equals("eval")).findFirst().get().getParameters()
                                    )
                            );
                        } catch (Exception ex) {
                            log.warning(String.format("Couldn't register function %s because %s", functionName, ex));
                            return null;
                        }
                    })
                    .filter(f -> f != null)
                    .map(e -> {
                        engine.getBindings(ScriptContext.GLOBAL_SCOPE).put("_" + e.getKey(), e.getValue().getLeft());
                        log.info(String.format("Registering %s as %s", e.getValue().getLeft().getClass(), e.getKey()));

                        Parameter[] params = e.getRight().getRight();
                        String paramNames = Stream.of(params).map(p -> p.getName()).collect(Collectors.joining(","));

                        return e.getKey() + "=function(" + paramNames + "){ return _" + e.getKey() + ".eval(" + paramNames + ");}";
                    }).collect(Collectors.joining(";"));

            engine.put("log", (Consumer<Object>) (s) -> log.info(s.toString()));

            String functionScript = "function " + getFunctionName() + "(message) { log(message); " + this.getScript() + "}; ";

            log.info(setupScript);
            log.info(functionScript);
            engine.eval(setupScript + ";" + functionScript, engine.getContext());

            initialized = true;
        } catch (
                ScriptException e) {
            throw new RuntimeException("Script invalid", e);
        }
    }

    private String getFunctionName() {
        if (this.$functionName.isEmpty()) {
            this.$functionName = SCORE_FUNCTION + "_" +
                    String.valueOf(Math.abs(this.script.hashCode())) + "_"
                    + String.valueOf(ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE));
        }
        return this.$functionName;
    }

    @Override
    public void close() {
    }

    boolean initialized = false;

    @Override
    public Map<String, Object> feed(Message message) {
        if (!initialized) open();
        try {
            return (Map<String, Object>) ((Invocable) engine).invokeFunction(getFunctionName(), extractMessage(message));
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
        engine.eval(script);
    }

    @Override
    public Object invokeFunction(String function, Object... args) throws ScriptException, NoSuchMethodException {
        return ((Invocable) engine).invokeFunction(function, args);
    }

    private HashMap<String, Object> extractMessage(Message message) {
        HashMap<String, Object> flat = new HashMap<>(message.getExtensions());
        flat.put("ts", message.getTs());
        flat.put("id", message.getId());
        return flat;
    }

    public static JavascriptEngineBuilder builder() {
        return new JavascriptEngineBuilder();
    }
}
