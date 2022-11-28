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

public interface RuleEngine {
    String getScript();
    void open();
    void close();
    boolean validate();
    Map<String, Object> feed(Message message);

    void eval(String script) throws ScriptException;

    Object invokeFunction(String function, Object... args) throws ScriptException, NoSuchMethodException;
}
