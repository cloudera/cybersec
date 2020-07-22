package com.cloudera.cyber.rules.engines;

import com.cloudera.cyber.Message;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.script.ScriptException;
import java.util.Map;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class StellarEngine implements RuleEngine {

    private String script;

    @Override
    public void open() {

    }

    @Override
    public void close() {

    }

    @Override
    public Map<String, Object> feed(Message message) {
        return null;
    }

    @Override
    public void eval(String script) throws ScriptException {

    }
}
