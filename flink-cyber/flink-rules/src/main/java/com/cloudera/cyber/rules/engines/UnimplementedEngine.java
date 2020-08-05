package com.cloudera.cyber.rules.engines;

import com.cloudera.cyber.Message;

import javax.script.ScriptException;
import java.util.Map;

public class UnimplementedEngine implements RuleEngine {
    @Override
    public String getScript() {
        return null;
    }

    @Override
    public void open() {
        throw new RuntimeException("RuleEngine not implemented yet");
    }

    @Override
    public void close() {

    }

    @Override
    public Map<String, Object> feed(Message message) {
        throw new RuntimeException("RuleEngine not implemented yet");
    }

    @Override
    public void eval(String script) throws ScriptException {
        throw new RuntimeException("RuleEngine not implemented yet");
    }

    @Override
    public Object invokeFunction(String function, Object... args) {
        throw new RuntimeException("RuleEngine not implemented yet");
    }
}
