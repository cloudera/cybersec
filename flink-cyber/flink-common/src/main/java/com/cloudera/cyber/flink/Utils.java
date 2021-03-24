package com.cloudera.cyber.flink;

import com.google.common.io.Resources;
import com.hortonworks.registries.schemaregistry.client.SchemaRegistryClient;
import lombok.extern.slf4j.Slf4j;
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
import java.security.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static com.hortonworks.registries.schemaregistry.client.SchemaRegistryClient.Configuration.SASL_JAAS_CONFIG;
import static org.apache.flink.configuration.GlobalConfiguration.loadConfiguration;

@Slf4j
public class Utils {
    public static final String KAFKA_PREFIX = "kafka.";
    private static final String MASK = "*";

    public static final String SENSITIVE_KEYS_KEY = "sensitive.keys";

    public static final String K_SCHEMA_REG_URL = "schema.registry.url";
    public static final String K_SCHEMA_REG_SSL_CLIENT_KEY = "schema.registry.client.ssl";
    public static final String K_SCHEMA_REG_SASL_JAAS_KEY = "schema.registry." + SASL_JAAS_CONFIG.name();
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

    private static AtomicInteger nextKafkaClientId = new AtomicInteger();

    private static Properties kafkaDefaultSettings(Properties kafkaProperties, String groupId, boolean consumer) {
        // interceptor currently unable to work with flink transactional kafka
        // https://docs.google.com/document/d/19jIN_POJvZPV466V5DolBKJxlqWOxYz-2gJV4e5cYtE/edit

//        kafkaProperties.put(consumer ? ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG : ProducerConfig.INTERCEPTOR_CLASSES_CONFIG,
//                consumer ?
//                        "com.hortonworks.smm.kafka.monitoring.interceptors.MonitoringConsumerInterceptor" :
//                        "com.hortonworks.smm.kafka.monitoring.interceptors.MonitoringProducerInterceptor");

        groupId = (String)kafkaProperties.getOrDefault(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        if (!consumer) {
            kafkaProperties.putIfAbsent(ProducerConfig.ACKS_CONFIG, "all");
            kafkaProperties.put(ProducerConfig.CLIENT_ID_CONFIG, String.format("%s-producer-%d", groupId, nextKafkaClientId.incrementAndGet()));
        } else {
            kafkaProperties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            kafkaProperties.put(ConsumerConfig.CLIENT_ID_CONFIG, String.format("%s-consumer-%d", groupId, nextKafkaClientId.incrementAndGet()));
        }

        log.info(String.format("Kafka Properties: %s", kafkaProperties));
        return kafkaProperties;
    }

    public static Properties readKafkaProperties(Properties properties, String groupId, boolean consumer) {
        return kafkaDefaultSettings(readProperties(properties, KAFKA_PREFIX), groupId, consumer);
    }

    public static Properties readKafkaProperties(Map<String, String> properties, String groupId, boolean consumer) {
        return kafkaDefaultSettings(readProperties(properties, KAFKA_PREFIX), groupId, consumer);
    }

    public static Properties readKafkaProperties(ParameterTool params, String groupId, boolean consumer) {
        return kafkaDefaultSettings(readProperties(params.getProperties(), KAFKA_PREFIX), groupId, consumer);
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
            log.info(String.format("Schema Registry param: %s=%s", key, isSensitive(key) ? MASK : schemaRegistryConf.get(key)));
        }
        return schemaRegistryConf;
    }

    public static Map<String, Object> readSchemaRegistryProperties(ParameterTool params) {
        Map<String, Object> schemaRegistryConf = new HashMap<>();
        String schemaRegistryUrl = params.getRequired(K_SCHEMA_REG_URL);
        schemaRegistryConf.put(SchemaRegistryClient.Configuration.SCHEMA_REGISTRY_URL.name(), schemaRegistryUrl);

        if (schemaRegistryUrl.startsWith("https")) {
            Map<String, String> sslClientConfig = new HashMap<>();
            String sslKey = K_SCHEMA_REG_SSL_CLIENT_KEY + "." + K_TRUSTSTORE_PATH;
            sslClientConfig.put(K_TRUSTSTORE_PATH, isSensitive(sslKey, params) ? decrypt(params.getRequired(sslKey)) : params.getRequired(sslKey));
            sslKey = K_SCHEMA_REG_SSL_CLIENT_KEY + "." + K_TRUSTSTORE_PASSWORD;
            sslClientConfig.put(K_TRUSTSTORE_PASSWORD, isSensitive(sslKey, params) ? decrypt(params.getRequired(sslKey)) : params.getRequired(sslKey));
            sslClientConfig.put(K_KEYSTORE_PASSWORD, ""); //ugly hack needed for SchemaRegistryClient

            schemaRegistryConf.put(K_SCHEMA_REG_SSL_CLIENT_KEY, sslClientConfig);
            String saslConfig = params.get(K_SCHEMA_REG_SASL_JAAS_KEY);
            if (saslConfig != null) {
                schemaRegistryConf.put(SASL_JAAS_CONFIG.name(), saslConfig);
            }
        }
        log.info("### Schema Registry parameters:");
        for (String key : schemaRegistryConf.keySet()) {
            log.info(String.format("Schema Registry param: %s=%s", key, isSensitive(key, params) ? MASK : schemaRegistryConf.get(key)));
        }
        return schemaRegistryConf;
    }


    public static String readResourceFile(String resourceLocation, Class<?> cls) {
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


    public static byte[] sign(String s, PrivateKey key) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature sig = Signature.getInstance("SHA1WithRSA");
        sig.initSign(key);
        sig.update(s.getBytes(StandardCharsets.UTF_8));
        return sig.sign();
    }

    public static boolean verify(String s, byte[] signature, PublicKey key) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature sig = Signature.getInstance("SHA1WithRSA");
        sig.initVerify(key);
        sig.update(s.getBytes(StandardCharsets.UTF_8));
        return sig.verify(signature);
    }


}
