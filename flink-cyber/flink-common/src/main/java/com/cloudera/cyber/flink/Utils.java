package com.cloudera.cyber.flink;

import com.google.common.io.Resources;
import lombok.extern.java.Log;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.client.cli.CliFrontend;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Preconditions;
import org.apache.flink.util.encrypttool.EncryptTool;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.apache.flink.configuration.GlobalConfiguration.loadConfiguration;

@Log
public class Utils {
    public static final String KAFKA_PREFIX = "kafka.";
    private static final String MASK = "*";

    public static final String SENSITIVE_KEYS_KEY = "sensitive.keys";

    public static final String K_SCHEMA_REG_URL = "schema.registry.url";
    public static final String K_SCHEMA_REG_SSL_CLIENT_KEY = "schema.registry.client.ssl";
    public static final String K_TRUSTSTORE_PATH = "trustStorePath";
    public static final String K_TRUSTSTORE_PASSWORD = "trustStorePassword";
    public static final String K_KEYSTORE_PASSWORD = "keyStorePassword";

    public static final String K_PROPERTIES_FILE = "properties.file";

    private static Properties readProperties(Properties properties, String prefix) {
        Properties targetProperties = new Properties();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                targetProperties.setProperty(key.substring(prefix.length()), properties.get(key).toString());
            }
        }
        return targetProperties;
    }

    private static Properties readProperties(Map<String, String> properties, String prefix) {
        Properties targetProperties = new Properties();
        for (String key : properties.keySet()) {
            if (key.startsWith(prefix)) {
                targetProperties.setProperty(key.substring(prefix.length()), properties.get(key));
            }
        }
        return targetProperties;
    }

    private static Properties kafkaDefaultSettings(Properties kafkaProperties, boolean consumer) {
        // interceptor currently unable to work with flink transactional kafka
        // https://docs.google.com/document/d/19jIN_POJvZPV466V5DolBKJxlqWOxYz-2gJV4e5cYtE/edit

//        kafkaProperties.put(consumer ? ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG : ProducerConfig.INTERCEPTOR_CLASSES_CONFIG,
//                consumer ?
//                        "com.hortonworks.smm.kafka.monitoring.interceptors.MonitoringConsumerInterceptor" :
//                        "com.hortonworks.smm.kafka.monitoring.interceptors.MonitoringProducerInterceptor");

        if (!consumer && !kafkaProperties.containsKey(ProducerConfig.ACKS_CONFIG)) {
            kafkaProperties.put(ProducerConfig.ACKS_CONFIG, "all");
        }
        log.info(String.format("Kafka Properties: %s", kafkaProperties));
        return kafkaProperties;
    }

    public static Properties readKafkaProperties(Properties properties, boolean consumer) {
        return kafkaDefaultSettings(readProperties(properties, KAFKA_PREFIX), consumer);
    }

    public static Properties readKafkaProperties(Map<String, String> properties, boolean consumer) {
        return kafkaDefaultSettings(readProperties(properties, KAFKA_PREFIX), consumer);
    }

    public static Properties readKafkaProperties(ParameterTool params, boolean consumer) {
        return kafkaDefaultSettings(readProperties(params.getProperties(), KAFKA_PREFIX), consumer);
    }

    public static Map<String, String> readSchemaRegistryProperties(Map<String, String> params) {
        Map<String, String> schemaRegistryConf = new HashMap<>();
        schemaRegistryConf.put(K_SCHEMA_REG_URL, params.get(K_SCHEMA_REG_URL));

        if (params.get(K_SCHEMA_REG_URL).startsWith("https")) {
            Map<String, String> sslClientConfig = new HashMap<>();
            String sslKey = K_SCHEMA_REG_SSL_CLIENT_KEY + "." + K_TRUSTSTORE_PATH;
            sslClientConfig.put(K_TRUSTSTORE_PATH, isSensitive(sslKey) ? decrypt(params.get(sslKey)) : params.get(sslKey));
            sslKey = K_SCHEMA_REG_SSL_CLIENT_KEY + "." + K_TRUSTSTORE_PASSWORD;
            sslClientConfig.put(K_TRUSTSTORE_PASSWORD, isSensitive(sslKey) ? decrypt(params.get(sslKey)) : params.get(sslKey));
            sslClientConfig.put(K_KEYSTORE_PASSWORD, ""); //ugly hack needed for SchemaRegistryClient

            //schemaRegistryConf.put(K_SCHEMA_REG_SSL_CLIENT_KEY, sslClientConfig);
        }
        log.info("### Schema Registry parameters:");
        for (String key : schemaRegistryConf.keySet()) {
            log.info(String.format("Schema Registry param: {}={}", key, isSensitive(key) ? MASK : schemaRegistryConf.get(key)));
        }
        return schemaRegistryConf;
    }

    public static Map<String, Object> readSchemaRegistryProperties(ParameterTool params) {
        Map<String, Object> schemaRegistryConf = new HashMap<>();
        schemaRegistryConf.put(K_SCHEMA_REG_URL, params.getRequired(K_SCHEMA_REG_URL));

        if (params.getRequired(K_SCHEMA_REG_URL).startsWith("https")) {
            Map<String, String> sslClientConfig = new HashMap<>();
            String sslKey = K_SCHEMA_REG_SSL_CLIENT_KEY + "." + K_TRUSTSTORE_PATH;
            sslClientConfig.put(K_TRUSTSTORE_PATH, isSensitive(sslKey, params) ? decrypt(params.getRequired(sslKey)) : params.getRequired(sslKey));
            sslKey = K_SCHEMA_REG_SSL_CLIENT_KEY + "." + K_TRUSTSTORE_PASSWORD;
            sslClientConfig.put(K_TRUSTSTORE_PASSWORD, isSensitive(sslKey, params) ? decrypt(params.getRequired(sslKey)) : params.getRequired(sslKey));
            sslClientConfig.put(K_KEYSTORE_PASSWORD, ""); //ugly hack needed for SchemaRegistryClient

            schemaRegistryConf.put(K_SCHEMA_REG_SSL_CLIENT_KEY, sslClientConfig);
        }
        log.info("### Schema Registry parameters:");
        for (String key : schemaRegistryConf.keySet()) {
            log.info(String.format("Schema Registry param: {}={}", key, isSensitive(key, params) ? MASK : schemaRegistryConf.get(key)));
        }
        return schemaRegistryConf;
    }


    public static String readResourceFile(String resourceLocation, Class cls) {
        try {
            return new String(Files.readAllBytes(Paths.get(cls.getResource(resourceLocation).toURI())));
        } catch (Exception e) {
            return null;
        }
    }


    public static boolean isSensitive(String key) {
        Preconditions.checkNotNull(key, "key is null");
        final String value = K_SCHEMA_REG_SSL_CLIENT_KEY + "." + K_TRUSTSTORE_PATH;
        String keyInLower = key.toLowerCase();
        String[] sensitiveKeys = value.split(",");

        for (int i = 0; i < sensitiveKeys.length; ++i) {
            String hideKey = sensitiveKeys[i];
            if (keyInLower.length() >= hideKey.length() && keyInLower.contains(hideKey)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSensitive(String key, ParameterTool params) {
        Preconditions.checkNotNull(key, "key is null");
        final String value = params.get(SENSITIVE_KEYS_KEY);
        if (value == null) {
            return false;
        }
        String keyInLower = key.toLowerCase();
        String[] sensitiveKeys = value.split(",");

        for (int i = 0; i < sensitiveKeys.length; ++i) {
            String hideKey = sensitiveKeys[i];
            if (keyInLower.length() >= hideKey.length() && keyInLower.contains(hideKey)) {
                return true;
            }
        }
        return false;
    }

    public static String decrypt(String input) {
        Preconditions.checkNotNull(input, "key is null");
        return EncryptTool.getInstance(getConfiguration()).decrypt(input);
    }

    public static Configuration getConfiguration() {
        return ConfigHolder.INSTANCE;
    }

    private static class ConfigHolder {
        static final Configuration INSTANCE = loadConfiguration(CliFrontend.getConfigurationDirectoryFromEnv());
    }


    public static String getResourceAsString(String file) {
        URL url = Resources.getResource(file);
        try {
            return Resources.toString(url, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }


    public static String readFile(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
