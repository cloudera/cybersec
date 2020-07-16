package com.cloudera.cyber.flink;

import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.utils.ParameterTool;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class Utils {
    public static final String KAFKA_PREFIX = "kafka.";

    public static Properties readKafkaProperties(ParameterTool params, boolean consumer) {
        Properties properties = new Properties();
        for (String key : params.getProperties().stringPropertyNames()) {
            if (key.startsWith(KAFKA_PREFIX)) {
                properties.setProperty(key.substring(KAFKA_PREFIX.length()), params.get(key));
            }
        }

        return properties;
    }

}
