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

import javax.script.ScriptException;
import java.util.Map;

public class UnimplementedEngine implements RuleEngine {
    @Override
    public String getScript() {
        return null;
    }

    @Override
    public void open() {
        throw new RuntimeException("RuleEngine not implemented yet");
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
        throw new RuntimeException("RuleEngine not implemented yet");
    }

    @Override
    public void eval(String script) {
        throw new RuntimeException("RuleEngine not implemented yet");
    }

    @Override
    public Object invokeFunction(String function, Object... args) {
        throw new RuntimeException("RuleEngine not implemented yet");
    }
}
