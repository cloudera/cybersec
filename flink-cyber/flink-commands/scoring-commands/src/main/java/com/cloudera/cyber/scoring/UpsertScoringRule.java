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

package com.cloudera.cyber.scoring;

import com.cloudera.cyber.flink.Utils;
import com.cloudera.cyber.rules.DynamicRuleCommandType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.base.Preconditions;
import com.hortonworks.registries.schemaregistry.serdes.avro.kafka.KafkaAvroDeserializer;
import com.hortonworks.registries.schemaregistry.serdes.avro.kafka.KafkaAvroSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TimeoutException;
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
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

@Slf4j
public class UpsertScoringRule {

    private static void fail(String message, Exception e) {
        log.error(message, e);
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
        Preconditions.checkArgument(args.length == 2, "Requires <property_file> <scoring_rule> parameters.");

        try {
            ParameterTool applicationProperties = ParameterTool.fromPropertiesFile(args[0]);
            ScoringRule ruleToUpsert = readScoringRule(args[1]);
            validateScoringRule(ruleToUpsert);
            String ruleInputTopic = applicationProperties.getRequired("query.input.topic");
            String ruleOutputTopic = applicationProperties.getRequired("query.output.topic");

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
        } catch (IOException e) {
            fail("Could not read config properties file: " + args[0]);
        } catch (ScriptException e) {
            fail("Invalid script", e);
        } catch (Exception e) {
            fail("Unable to send rule", e);
        }
    }

    private static void produce(ScoringRuleCommand command, ParameterTool applicationProperties, String topic) {
        Properties producerProperties = Utils.readKafkaProperties(applicationProperties, "rule-config-console", false);
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        producerProperties.putAll(Utils.readSchemaRegistryProperties(applicationProperties));
        try (KafkaProducer<String, ScoringRuleCommand> producer = new KafkaProducer<>(producerProperties)) {
            ProducerRecord<String, ScoringRuleCommand> rec = new ProducerRecord<>(topic, command);
            producer.send(rec, producerResult);
        } catch (Exception e) {
            fail("Kafka producer is available", e);
        }
    }

    private static void consume(String commandId, ParameterTool applicationProperties, String topic) {
        Properties consumerProperties = Utils.readKafkaProperties(applicationProperties, "rule-config-console", true);
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "flink_cyber_command_line".concat(UUID.randomUUID().toString()));
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProperties.put("specific.avro.reader", true);
        consumerProperties.putAll(Utils.readSchemaRegistryProperties(applicationProperties));

        final int maxRetries = applicationProperties.getInt("kafka.retry.amount", 10);
        final int retryDuration = applicationProperties.getInt("kafka.retry.duration", 5);

        int retries = 0;

        try (KafkaConsumer<String, ScoringRuleCommandResult> consumer = new KafkaConsumer<>(consumerProperties)) {
            boolean gotResponse = false;
            while (!gotResponse && retries <= maxRetries) {
                consumer.subscribe(Collections.singletonList(topic));
                ConsumerRecords<String, ScoringRuleCommandResult> records = consumer.poll(Duration.ofSeconds(retryDuration));
                for (ConsumerRecord<String, ScoringRuleCommandResult> rec : records) {
                    boolean responseMatches = commandId.equals(rec.value().getCmdId());
                    gotResponse = gotResponse || responseMatches;
                    log.debug("Match=" + responseMatches + ", Response; " + rec.value());
                }
                retries++;
            }
            if (!gotResponse){
                fail(String.format("Kafka read timed out after %s attempts", retries), new TimeoutException());
            }
        } catch (Exception e) {
            fail("Kafka consumer is available", e);
        }
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

