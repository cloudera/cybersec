package com.cloudera.cyber.stellar;

import org.apache.metron.parsers.BasicParser;
import org.json.simple.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ExampleMetronParser extends BasicParser {
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
