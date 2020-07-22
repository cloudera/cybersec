package com.cloudera.cyber.rules;

import com.cloudera.cyber.rules.engines.*;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

public enum RuleType {
    JS(new JavascriptEngineBuilder()),
    PYTHON(new PythonEngineBuilder()),
    STELLAR(new StellarEngineBuilder());

    private final RuleEngineBuilder engineBuilder;
    private Cache<String, RuleEngine> engineCache;

    private RuleType(RuleEngineBuilder engineBuilder) {
        this.engineBuilder = engineBuilder;
        engineCache = Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.MINUTES)
                .maximumSize(100)
                .build();
    }

    /**
     * Create a cached rule engine based on the script
     *
     * @param ruleScript
     * @return
     */
    public RuleEngine engine(String ruleScript) {
        return engineCache.get(ruleScript, s -> engineBuilder.script(s).build());
    }
}
