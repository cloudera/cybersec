package com.cloudera.cyber.scoring;

import com.cloudera.cyber.flink.Utils;
import com.cloudera.cyber.rules.DynamicRuleCommandType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.base.Preconditions;
import com.hortonworks.registries.schemaregistry.serdes.avro.kafka.KafkaAvroDeserializer;
import com.hortonworks.registries.schemaregistry.serdes.avro.kafka.KafkaAvroSerializer;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import javax.script.ScriptException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

public class UpsertScoringRule {

    private static void fail(String message, Exception e) {
        System.err.println(message);
        if (e != null) {
            e.printStackTrace();
        }
        System.exit(1);
    }

    private static void fail(String message) {
        fail(message, null);
    }

    private static ScoringRule readScoringRule(String pathToRuleJson) throws IOException {
        try (InputStream jsonRuleStream = new FileInputStream(new File(pathToRuleJson))) {
            ObjectMapper objectMapper = new ObjectMapper();
            JavaTimeModule module = new JavaTimeModule();
            objectMapper.registerModule(module);
            ObjectReader scoringRuleMapper = objectMapper.readerFor(ScoringRule.class);
            return scoringRuleMapper.readValue(jsonRuleStream);
        }
    }

    public static void main(String[] args) {
        Preconditions.checkArgument(args.length == 2, "Should contain 2 args.");
        Properties applicationProperties = null;
        try {
            ParameterTool params = ParameterTool.fromPropertiesFile(args[0]);
            applicationProperties = params.getProperties();
        } catch (IOException e) {
            fail("Could not read config properties file: " + args[0]);
        }

        try {
            ScoringRule ruleToUpsert = readScoringRule(args[1]);
            validateScoringRule(ruleToUpsert);
            String ruleInputTopic = applicationProperties.getProperty("query.input.topic");
            Preconditions.checkNotNull(ruleInputTopic, "Specify rule input topic by setting 'query.input.topic' in the properties file.");
            String ruleOutputTopic = applicationProperties.getProperty("query.output.topic");
            Preconditions.checkNotNull(ruleOutputTopic, "Specify rule input topic by setting 'query.output.topic' in the properties file.");

            String commandId = UUID.randomUUID().toString();
            final DynamicRuleCommandType dynamicRuleCommandType = Optional.ofNullable(applicationProperties.get("rule.command.type"))
                    .map(Object::toString)
                    .map(DynamicRuleCommandType::valueOf)
                    .orElse(DynamicRuleCommandType.UPSERT);

            ScoringRuleCommand command = ScoringRuleCommand.builder().
                    id(commandId).
                    type(dynamicRuleCommandType).
                    ts(Instant.now().toEpochMilli()).
                    ruleId(ruleToUpsert.getId()).
                    rule(ruleToUpsert).
                    headers(Collections.emptyMap()).
                    build();

            produce(command, applicationProperties, ruleInputTopic);
            consume(commandId, applicationProperties, ruleOutputTopic);
        } catch (ScriptException e) {
            fail("Invalid script", e);
        } catch (Exception e) {
            fail("Unable to send rule", e);
        }
    }

    private static void produce(ScoringRuleCommand command, Properties applicationProperties, String topic) {
        Properties producerProperties = Utils.readKafkaProperties(applicationProperties, "rule-config-console", false);
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        producerProperties.putAll(readSchemaRegistryProperties((Map) applicationProperties));
        try (KafkaProducer<String, ScoringRuleCommand> producer = new KafkaProducer<>(producerProperties)) {
            ProducerRecord<String, ScoringRuleCommand> rec = new ProducerRecord<>(topic, command);
            producer.send(rec, producerResult);
        } catch (Exception e) {
            fail("Kafka producer is available", e);
        }
    }

    private static void consume(String commandId, Properties applicationProperties, String topic) {
        Properties consumerProperties = Utils.readKafkaProperties(applicationProperties, "rule-config-console", true);
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "flink_cyber_command_line".concat(UUID.randomUUID().toString()));
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProperties.put("specific.avro.reader", true);
        consumerProperties.putAll(readSchemaRegistryProperties((Map) applicationProperties));

        try (KafkaConsumer<String, ScoringRuleCommandResult> consumer = new KafkaConsumer<>(consumerProperties)) {
            boolean gotResponse = false;
            while (!gotResponse) {
                consumer.subscribe(Collections.singletonList(topic));
                ConsumerRecords<String, ScoringRuleCommandResult> records = consumer.poll(Duration.ofSeconds(5));
                for (ConsumerRecord<String, ScoringRuleCommandResult> rec : records) {
                    boolean responseMatches = commandId.equals(rec.value().getCmdId());
                    gotResponse = gotResponse || responseMatches;
                    System.out.println("Match=" + responseMatches + ", Response; " + rec.value());
                }
            }
        } catch (Exception e) {
            fail("Kafka consumer is available", e);
        }
    }

    public static final String K_SCHEMA_REG_URL = "schema.registry.url";
    public static final String K_SCHEMA_REG_SSL_CLIENT_KEY = "schema.registry.client.ssl";
    public static final String K_TRUSTSTORE_PATH = "trustStorePath";
    public static final String K_TRUSTSTORE_PASSWORD = "trustStorePassword";
    public static final String K_KEYSTORE_PASSWORD = "keyStorePassword";

    public static Map<String, Object> readSchemaRegistryProperties(Map<String, String> params) {
        Map<String, Object> schemaRegistryConf = new HashMap<>();
        schemaRegistryConf.put(K_SCHEMA_REG_URL, params.get(K_SCHEMA_REG_URL));

        if (params.get(K_SCHEMA_REG_URL).startsWith("https")) {
            Map<String, String> sslClientConfig = new HashMap<>();
            String sslKey = K_SCHEMA_REG_SSL_CLIENT_KEY + "." + K_TRUSTSTORE_PATH;
            sslClientConfig.put(K_TRUSTSTORE_PATH, params.get(sslKey));
            sslKey = K_SCHEMA_REG_SSL_CLIENT_KEY + "." + K_TRUSTSTORE_PASSWORD;
            sslClientConfig.put(K_TRUSTSTORE_PASSWORD, params.get(sslKey));
            sslClientConfig.put(K_KEYSTORE_PASSWORD, ""); //ugly hack needed for SchemaRegistryClient

            schemaRegistryConf.put(K_SCHEMA_REG_SSL_CLIENT_KEY, sslClientConfig);
        }
        System.out.println("### Schema Registry parameters:");
        schemaRegistryConf.keySet().forEach(key ->
                System.out.printf("Schema Registry param: %s=%s%n", key, schemaRegistryConf.get(key).toString()));
        return schemaRegistryConf;
    }


    private static final Callback producerResult = (recordMetadata, e) -> {
        if (e != null) {
            fail("producer failed", e);
        }
    };

    private static void validateScoringRule(ScoringRule ruleToUpsert) throws ScriptException {
        if (ruleToUpsert.getTsStart() == null) {
            fail("Start date cannot be null." + ruleToUpsert);
        }

        if (ruleToUpsert.getTsEnd() == null) {
            fail("End date cannot be null." + ruleToUpsert);
        }

        if (ruleToUpsert.getType() == null) {
            fail("Rule type cannot be null." + ruleToUpsert);
        }

        if (ruleToUpsert.getRuleScript() == null) {
            fail("Rule script cannot be null." + ruleToUpsert);
        }

        ruleToUpsert.getType().engine(ruleToUpsert.getRuleScript()).eval("function test(message) { " + ruleToUpsert.getRuleScript() + " } ");
    }
}

