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

import com.cloudera.cyber.rules.engines.JavaScriptGraaljsEngineBuilder;
import com.cloudera.cyber.rules.engines.JavaScriptNashornEngineBuilder;
import com.cloudera.cyber.rules.engines.PythonEngineBuilder;
import com.cloudera.cyber.rules.engines.RuleEngine;
import com.cloudera.cyber.rules.engines.RuleEngineBuilder;
import com.cloudera.cyber.rules.engines.StellarEngineBuilder;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import lombok.Getter;

public enum RuleType {
    JS_GRAAL(new JavaScriptGraaljsEngineBuilder()),
    //JS_NASHORN is going to be removed after switch to JDK 17.
    // It's recommended to switch to JS_GRAAL instead, or rely on the JS that will get replaced with JS_GRAAL once JS_NASHORN is removed
    @Deprecated
    JS_NASHORN(new JavaScriptNashornEngineBuilder()),
    //will use the first valid engine
    JS(Stream.of(JS_NASHORN, JS_GRAAL)
          .map(RuleType::getEngineBuilder)
          .filter(RuleEngineBuilder::isValid)
          .findFirst()
          .orElseThrow(() -> new RuntimeException("No valid JS engine was found!"))),
    PYTHON(new PythonEngineBuilder()),
    STELLAR(new StellarEngineBuilder());

    @Getter
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
     * Create a cached rule engine based on the script.
     */
    public RuleEngine engine(String ruleScript) {
        return engineCache.get(ruleScript, s -> engineBuilder.script(s).build());
    }

}
