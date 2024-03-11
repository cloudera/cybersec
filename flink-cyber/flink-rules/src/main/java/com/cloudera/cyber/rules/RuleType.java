/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

package com.cloudera.cyber.rules;

import com.cloudera.cyber.rules.engines.*;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

public enum RuleType {
    JS(new JavaScriptGraaljsEngineBuilder()),
    @Deprecated
    JS_NASHORN(new JavaScriptNashornEngineBuilder()),
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
