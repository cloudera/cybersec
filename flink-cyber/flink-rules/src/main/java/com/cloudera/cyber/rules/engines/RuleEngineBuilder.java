package com.cloudera.cyber.rules.engines;

import lombok.Getter;

@Getter
public abstract class RuleEngineBuilder<T extends RuleEngine> {

    protected String script;

    public RuleEngineBuilder script(String script) {
        this.script = script;
        return this;
    }

    public abstract T build();
}
