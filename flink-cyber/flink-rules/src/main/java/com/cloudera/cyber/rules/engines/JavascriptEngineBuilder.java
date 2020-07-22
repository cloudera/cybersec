package com.cloudera.cyber.rules.engines;

import lombok.*;

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
