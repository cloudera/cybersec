package com.cloudera.cyber.rules.engines;

import com.cloudera.cyber.Message;

import javax.script.ScriptException;
import java.util.Map;

public interface RuleEngine {
    String getScript();
    void open();
    void close();
    Map<String, Object> feed(Message message);

    void eval(String script) throws ScriptException;

    Object invokeFunction(String function, Object... args) throws ScriptException, NoSuchMethodException;
}
