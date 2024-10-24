package com.cloudera.cyber.rules.engines;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.libs.CyberFunctionDefinition;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.script.ScriptException;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public abstract class JavaScriptEngine implements RuleEngine {

    protected static final String SCORE_FUNCTION = "score";
    protected static final String setupScript =
          CyberFunctionDefinition.findAll().map(JavaScriptEngine::generateJavascript).collect(Collectors.joining(";"));

    @NonNull
    protected final String script;
    protected final String functionName;

    public JavaScriptEngine(String script) {
        this.script = script;
        this.functionName = SCORE_FUNCTION + "_"
                            + Math.abs(this.script.hashCode()) + "_"
                            + ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
    }

    private static String generateJavascript(CyberFunctionDefinition functionDefinition) {
        String paramNames =
              functionDefinition.getParameters().stream().map(Parameter::getName).collect(Collectors.joining(","));
        String functionName = functionDefinition.getFunctionName();
        return functionName + "=function(" + paramNames + "){ return _" + functionName + ".eval(" + paramNames + ");}";
    }

    protected static void initBindings(BiConsumer<String, Object> bindingConsumer) {
        CyberFunctionDefinition.findAll().forEach(func -> {
            String udfFunctionName = func.getFunctionName();
            Class<?> implementationClass = func.getImplementationClass();
            try {
                log.info("Registering {} as {}", implementationClass, udfFunctionName);
                bindingConsumer.accept("_".concat(udfFunctionName),
                      implementationClass.getDeclaredConstructor().newInstance());
                log.info("Successfully registered {} as {}", implementationClass, udfFunctionName);
            } catch (Exception e) {
                log.debug(String.format("Could not register %s as %s", implementationClass, udfFunctionName), e);
            }
        });

        bindingConsumer.accept("log", (Consumer<Object>) (s) -> log.info(s.toString()));
    }

    @Override
    public void open() {
    }

    @Override
    public Map<String, Object> feed(Message message) {
        try {
            return (Map<String, Object>) invokeFunction(getFunctionName(), extractMessage(message));
        } catch (ScriptException e) {
            throw new RuntimeException("Javascript Engine function failed", e);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Javascript Engine function build invalid", e);
        } catch (ClassCastException e) {
            throw new RuntimeException("Javascript Engine function returned invalid structure", e);
        }
    }

    protected HashMap<String, Object> extractMessage(Message message) {
        HashMap<String, Object> flat = new HashMap<>(message.getExtensions());
        flat.put("ts", message.getTs());
        flat.put("id", message.getId());
        flat.put("source", message.getSource());
        return flat;
    }

}
