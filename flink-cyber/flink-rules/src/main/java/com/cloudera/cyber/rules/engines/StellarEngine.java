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

import com.cloudera.cyber.Message;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class StellarEngine implements RuleEngine {

    private String script;

    @Override
    public void open() {

    }

    @Override
    public boolean validate() {
        return true;
    }

    @Override
    public void close() {

    }

    @Override
    public Map<String, Object> feed(Message message) {
        return null;
    }

    @Override
    public void eval(String script) {

    }

    @Override
    public Object invokeFunction(String function, Object... args) {
        throw new RuntimeException("RuleEngine not implemented yet");
    }
}
