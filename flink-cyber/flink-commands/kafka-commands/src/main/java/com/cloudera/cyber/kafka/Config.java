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

package com.cloudera.cyber.kafka;

import static com.hortonworks.registries.schemaregistry.client.SchemaRegistryClient.Configuration.SASL_JAAS_CONFIG;

import com.hortonworks.registries.schemaregistry.client.SchemaRegistryClient;
import com.hortonworks.registries.schemaregistry.serdes.avro.kafka.KafkaAvroDeserializer;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;

public class Config {

    public static final String K_SCHEMA_REG_URL = "schema.registry.url";
    public static final String K_SCHEMA_REG_SSL_CLIENT_KEY = "schema.registry.client.ssl";
    public static final String K_SCHEMA_REG_SASL_JAAS_KEY = "schema.registry." + SASL_JAAS_CONFIG.name();
    public static final String K_TRUSTSTORE_PATH = "trustStorePath";
    public static final String K_TRUSTSTORE_PASSWORD = "trustStorePassword";
    public static final String K_KEYSTORE_PASSWORD = "keyStorePassword";
    public static final String PRINT_PROPERTY_PREFIX = "print.";

    private static final String KAFKA_PREFIX = "kafka.";
    public static final String PRINT_KAFKA_PROPERTY_PREFIX = PRINT_PROPERTY_PREFIX.concat(KAFKA_PREFIX);

    private final Properties properties = new Properties();

    public void load(String configFilePath) throws IOException {
        try (FileInputStream fileStream = new FileInputStream(configFilePath)) {
            properties.load(fileStream);
        }
    }

    public Properties getKafkaConsumerProperties() {
        Properties kafkaConsumerProperties = getPropertiesWithKafkaPrefix();
        kafkaConsumerProperties.putAll(readSchemaRegistryProperties(properties));
        kafkaConsumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        kafkaConsumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
              KafkaAvroDeserializer.class.getName());

        overridePrintDefault(kafkaConsumerProperties, ConsumerConfig.GROUP_ID_CONFIG,
              "flink_cyber_command_line".concat(UUID.randomUUID().toString()));
        overridePrintDefault(kafkaConsumerProperties, ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        kafkaConsumerProperties.put("specific.avro.reader", false);

        return kafkaConsumerProperties;
    }

    private void overridePrintDefault(Properties kafkaConsumerProperties, String kafkaProperty, String defaultValue) {
        kafkaConsumerProperties.put(kafkaProperty,
              properties.getOrDefault(PRINT_KAFKA_PROPERTY_PREFIX.concat(kafkaProperty), defaultValue));
    }

    private Properties getPropertiesWithKafkaPrefix() {
        Properties filteredProperties = new Properties();
        int prefixLength = Config.KAFKA_PREFIX.length();

        properties.stringPropertyNames().stream().filter(k -> k.startsWith(Config.KAFKA_PREFIX))
                  .forEach(k -> filteredProperties.setProperty(k.substring(prefixLength), properties.getProperty(k)));

        return filteredProperties;
    }

    private Map<String, Object> readSchemaRegistryProperties(Properties params) {
        Map<String, Object> schemaRegistryConf = new HashMap<>();
        String schemaRegistryUrl = getRequired(K_SCHEMA_REG_URL);
        schemaRegistryConf.put(SchemaRegistryClient.Configuration.SCHEMA_REGISTRY_URL.name(), schemaRegistryUrl);

        if (schemaRegistryUrl.startsWith("https")) {
            Map<String, String> sslClientConfig = new HashMap<>();
            String sslKey = K_SCHEMA_REG_SSL_CLIENT_KEY + "." + K_TRUSTSTORE_PATH;
            sslClientConfig.put(K_TRUSTSTORE_PATH, getRequired(sslKey));
            sslKey = K_SCHEMA_REG_SSL_CLIENT_KEY + "." + K_TRUSTSTORE_PASSWORD;
            sslClientConfig.put(K_TRUSTSTORE_PASSWORD, getRequired(sslKey));
            sslClientConfig.put(K_KEYSTORE_PASSWORD, ""); //ugly hack needed for SchemaRegistryClient

            schemaRegistryConf.put(K_SCHEMA_REG_SSL_CLIENT_KEY, sslClientConfig);
            String saslConfig = params.getProperty(K_SCHEMA_REG_SASL_JAAS_KEY);
            if (saslConfig != null) {
                schemaRegistryConf.put(SASL_JAAS_CONFIG.name(), saslConfig);
            }
        }
        return schemaRegistryConf;
    }

    public String getRequired(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new RuntimeException(String.format("Required property '%s' is not set", key));
        }
        return value;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String stringValue = properties.getProperty(key, Boolean.toString(defaultValue));
        return Boolean.parseBoolean(stringValue);
    }

    public int getInteger(String key, int defaultValue) {
        String stringValue = properties.getProperty(key, Integer.toString(defaultValue));
        return Integer.parseInt(stringValue);
    }
}
