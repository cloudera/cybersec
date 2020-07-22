package com.cloudera.cyber.flink;

import org.apache.flink.api.java.utils.ParameterTool;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Properties;
import java.util.Map;

public class Utils {
    public static final String KAFKA_PREFIX = "kafka.";
    public static final String SCHEMA_REGISTRY_PREFIX = "schema-registry.";

    private static Properties readProperties(Properties properties, String prefix) {
        Properties targetProperties = new Properties();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                targetProperties.setProperty(key.substring(prefix.length()), properties.get(key).toString());
            }
        }
        return targetProperties;
    }

    private static Properties readProperties(Map<String,String> properties, String prefix) {
        Properties targetProperties = new Properties();
        for (String key : properties.keySet()) {
            if (key.startsWith(prefix)) {
                targetProperties.setProperty(key.substring(prefix.length()), properties.get(key));
            }
        }
        return targetProperties;
    }

    public static Properties readKafkaProperties(Properties properties, boolean consumer) {
        return readProperties(properties, KAFKA_PREFIX);
    }
    public static Properties readKafkaProperties(Map<String,String> properties, boolean consumer) {
        return readProperties(properties, KAFKA_PREFIX);
    }
    public static Properties readKafkaProperties(ParameterTool params, boolean consumer) {
        return readProperties(params.getProperties(), KAFKA_PREFIX);
    }

    public static Map<String,String> readSchemaRegistryProperties(Map<String,String> properties) {
        Map<String,String> targetProperties = new HashMap<>();
        for (String key : properties.keySet()) {
            if (key.startsWith(SCHEMA_REGISTRY_PREFIX)) {
                targetProperties.put(
                        key.substring(SCHEMA_REGISTRY_PREFIX.length()),
                        properties.get(key).toString());
            }
        }
        return targetProperties;
    }

    public static String readResourceFile(String resourceLocation, Class cls) {
        try {
            return new String(Files.readAllBytes(Paths.get(cls.getResource(resourceLocation).toURI())));
        } catch (Exception e) {
            return null;
        }
    }

}
