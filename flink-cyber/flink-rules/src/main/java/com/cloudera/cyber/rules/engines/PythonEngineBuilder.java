package com.cloudera.cyber.rules.engines;

public class PythonEngineBuilder extends RuleEngineBuilder<UnimplementedEngine> {
    @Override
    public UnimplementedEngine build() {
        return new UnimplementedEngine();
    }
}
