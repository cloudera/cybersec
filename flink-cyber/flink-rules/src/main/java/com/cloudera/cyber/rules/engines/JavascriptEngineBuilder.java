package com.cloudera.cyber.rules.engines;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode
public class JavascriptEngineBuilder extends RuleEngineBuilder<JavaScriptEngine> {
    @Override
    public JavaScriptEngine build() {
        return new JavaScriptEngine(getScript());
    }
}
