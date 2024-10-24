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

package com.cloudera.cyber.flink;

import static com.hortonworks.registries.schemaregistry.client.SchemaRegistryClient.Configuration.SASL_JAAS_CONFIG;
import static org.apache.flink.configuration.GlobalConfiguration.loadConfiguration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import com.hortonworks.registries.schemaregistry.client.SchemaRegistryClient;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.client.cli.CliFrontend;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.fs.FSDataInputStream;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;
import org.apache.flink.util.Preconditions;
import org.apache.flink.util.encrypttool.EncryptTool;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;

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
    public static final String PASSWORD = "password";

    public static final ObjectMapper MAPPER = new ObjectMapper();

    public static Properties readProperties(Properties properties, String prefix) {
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

        //kafkaProperties.put(consumer ? ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG : ProducerConfig.INTERCEPTOR_CLASSES_CONFIG,
        //        consumer ?
        //                "com.hortonworks.smm.kafka.monitoring.interceptors.MonitoringConsumerInterceptor" :
        //                "com.hortonworks.smm.kafka.monitoring.interceptors.MonitoringProducerInterceptor");

        groupId = (String) kafkaProperties.getOrDefault(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        if (!consumer) {
            kafkaProperties.putIfAbsent(ProducerConfig.ACKS_CONFIG, "all");
            kafkaProperties.put(ProducerConfig.CLIENT_ID_CONFIG,
                  String.format("%s-producer-%d", groupId, nextKafkaClientId.incrementAndGet()));
        } else {
            kafkaProperties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            kafkaProperties.put(ConsumerConfig.CLIENT_ID_CONFIG,
                  String.format("%s-consumer-%d", groupId, nextKafkaClientId.incrementAndGet()));
        }

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
            sslClientConfig.put(K_TRUSTSTORE_PATH,
                  isSensitive(sslKey) ? decrypt(params.get(sslKey)) : params.get(sslKey));
            sslKey = K_SCHEMA_REG_SSL_CLIENT_KEY + "." + K_TRUSTSTORE_PASSWORD;
            sslClientConfig.put(K_TRUSTSTORE_PASSWORD,
                  isSensitive(sslKey) ? decrypt(params.get(sslKey)) : params.get(sslKey));
            sslClientConfig.put(K_KEYSTORE_PASSWORD, ""); //ugly hack needed for SchemaRegistryClient

            //schemaRegistryConf.put(K_SCHEMA_REG_SSL_CLIENT_KEY, sslClientConfig);
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
            sslClientConfig.put(K_TRUSTSTORE_PATH,
                  isSensitive(sslKey, params) ? decrypt(params.getRequired(sslKey)) : params.getRequired(sslKey));
            sslKey = K_SCHEMA_REG_SSL_CLIENT_KEY + "." + K_TRUSTSTORE_PASSWORD;
            sslClientConfig.put(K_TRUSTSTORE_PASSWORD,
                  isSensitive(sslKey, params) ? decrypt(params.getRequired(sslKey)) : params.getRequired(sslKey));
            sslClientConfig.put(K_KEYSTORE_PASSWORD, ""); //ugly hack needed for SchemaRegistryClient

            schemaRegistryConf.put(K_SCHEMA_REG_SSL_CLIENT_KEY, sslClientConfig);
            String saslConfig = params.get(K_SCHEMA_REG_SASL_JAAS_KEY);
            if (saslConfig != null) {
                schemaRegistryConf.put(SASL_JAAS_CONFIG.name(), saslConfig);
            }
        }
        return schemaRegistryConf;
    }

    private static <T> T jsonToObject(String json, TypeReference<T> typeReference) throws JsonProcessingException {
        return MAPPER.readValue(json, typeReference);
    }

    public static <T> T readResourceFile(String resourceLocation, Class<?> cls, TypeReference<T> typeReference)
          throws IOException {
        return jsonToObject(readResourceFile(resourceLocation, cls), typeReference);
    }

    public static String readResourceFile(String resourceLocation, Class<?> cls) {
        try {
            log.info("Try open file at {} for class {}", resourceLocation, cls);
            URL resource = cls.getResource(resourceLocation);
            if (resource == null) {
                log.info("Try open file with class loader at {} for class {}", resourceLocation, cls);
                resource = cls.getClassLoader().getResource(resourceLocation);
            }
            Preconditions.checkNotNull(resource, "resource is null");
            return IOUtils.toString(resource.openStream(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
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
        if (key.toLowerCase().contains(PASSWORD)) {
            return true;
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

    public static boolean isTimeEqual(Long unit1, String unitType1, Long unit2, String unitType2) {
        if (unit1 == null && unitType1 == null && unit2 == null && unitType2 == null) {
            return true;
        } else if (unit1 == null || unitType1 == null || unit2 == null || unitType2 == null) {
            return false;
        }
        return TimeUnit.valueOf(unitType1).toMillis(unit1) == TimeUnit.valueOf(unitType2).toMillis(unit2);
    }

    public static <T> boolean isTimeEqual(T object1, T object2, ToLongFunction<T> timeUnitSelector,
                                          Function<T, String> timeUnitTypeSelector) {
        return isTimeEqual(timeUnitSelector.applyAsLong(object1), timeUnitTypeSelector.apply(object1),
              timeUnitSelector.applyAsLong(object2), timeUnitTypeSelector.apply(object2));
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

    public static ParameterTool getParamToolsFromProperties(String[] pathToPropertyFiles) {
        return Arrays.stream(pathToPropertyFiles)
              .filter(pathToPropertyFile -> pathToPropertyFile.endsWith(".properties")).map(Path::new)
              .reduce(ParameterTool.fromMap(new HashMap<>()), (parameterTool, path) -> {
                  try {
                      FileSystem fileSystem = path.getFileSystem();
                      if (fileSystem.exists(path)) {
                          try (FSDataInputStream fsDataInputStream = fileSystem.open(path)) {
                              ParameterTool nextParamTool = ParameterTool.fromPropertiesFile(fsDataInputStream);
                              return parameterTool.mergeWith(nextParamTool);
                          }
                      }
                  } catch (IOException e) {
                      throw new RuntimeException(e);
                  }
                  return parameterTool;
              }, ParameterTool::mergeWith);
    }


    public static <T> T readFile(String path, TypeReference<T> typeReference) throws IOException {
        return jsonToObject(readFile(path), typeReference);
    }

    public static String readFile(String path) throws IOException {
        final Path filePath = new Path(path);
        final FileSystem fileSystem = filePath.getFileSystem();
        try (FSDataInputStream fsDataInputStream = fileSystem.open(filePath)) {
            return IOUtils.toString(fsDataInputStream, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Wasn't able to read file [%s]!", filePath), e);
        }
    }


    public static byte[] sign(String s, PrivateKey key)
          throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature sig = Signature.getInstance("SHA1WithRSA");
        sig.initSign(key);
        sig.update(s.getBytes(StandardCharsets.UTF_8));
        return sig.sign();
    }

    public static boolean verify(String s, byte[] signature, PublicKey key)
          throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature sig = Signature.getInstance("SHA1WithRSA");
        sig.initVerify(key);
        sig.update(s.getBytes(StandardCharsets.UTF_8));
        return sig.verify(signature);
    }


}
