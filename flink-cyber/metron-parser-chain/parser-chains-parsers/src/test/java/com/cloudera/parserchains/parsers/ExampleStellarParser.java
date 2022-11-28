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

package com.cloudera.parserchains.parsers;

import org.apache.metron.parsers.BasicParser;
import org.json.simple.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ExampleStellarParser extends BasicParser {
    private boolean initialized = false;
    private Map<String, Object> config;

    @Override
    public void init() {
        initialized = true;
    }

    @Override
    public void configure(Map<String, Object> config) {
        this.config = config;
    }

    @Override
    public List<JSONObject> parse(byte[] rawMessage) {


        String originalString = new String(rawMessage);
        if (originalString.equals("null")) {
            return null;
        } else if (originalString.equals("throw")) {
            throw new RuntimeException("Example exception");
        } else if (originalString.equals("empty")) {
            return Collections.emptyList();
        } else {
            JSONObject parsedMessage = new JSONObject();
            String[] parts = originalString.split("\\s+");
            parsedMessage.put("timestamp", Long.valueOf(parts[0]));
            for (int i = 1; i < parts.length; i++) {
                parsedMessage.put(String.format("column%d", i), parts[i]);
            }
            parsedMessage.put("initialized", initialized);
            parsedMessage.putAll(config);

            return Collections.singletonList(parsedMessage);
        }
    }
}
