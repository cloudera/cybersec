package com.cloudera.cyber.rules.engines;

public class StellarEngineBuilder extends RuleEngineBuilder<StellarEngine> {
    @Override
    public StellarEngine build() {
        return new StellarEngine(script);
    }
}
