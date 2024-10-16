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

package com.cloudera.cyber.rules.engines;

import lombok.Getter;

@Getter
public abstract class RuleEngineBuilder<T extends RuleEngine> {

    protected String script;

    public RuleEngineBuilder script(String script) {
        this.script = script;
        return this;
    }

    /**
     * This method identifies if the underlying rule engine is supported by the current environment.
     * It should be overridden in case some logic is need, otherwise it returns true.
     *
     * @return true if this builder is valid for the environment
     */
    public boolean isValid() {
        return true;
    }

    public abstract T build();
}
