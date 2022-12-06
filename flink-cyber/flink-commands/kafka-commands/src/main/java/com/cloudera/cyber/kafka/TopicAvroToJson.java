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

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

@Slf4j
public class TopicAvroToJson {

    private static void fail(String message, Exception e) {
        log.error(message, e);
        System.exit(1);
    }

    private static void fail(String message) {
        fail(message, null);
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            fail("Requires <kafka_property_file> <topic> parameters.");
        }

        String applicationPropertiesFile = args[0];
        String topicToDump = args[1];

        Config config = new Config();
        try {
            config.load(applicationPropertiesFile);
            boolean prettyOutput = config.getBoolean("print.output.pretty", true);
            int maxRecords = config.getInteger("print.output.max.records", 500);
            int maxRetries = config.getInteger("print.max.retries", 5);

            consume(config.getKafkaConsumerProperties(), topicToDump, prettyOutput, maxRetries, maxRecords);
        } catch (IOException e) {
            fail("Could not read config properties file: " + args[0]);
        } catch (Exception e) {
            fail("Unable to send rule", e);
        }
    }

    private static void consume(Properties consumerProperties, String topic, boolean pretty, int maxRetries, int maxRecords) {

        try (KafkaConsumer<String, GenericRecord> consumer = new KafkaConsumer<>(consumerProperties)) {
            boolean gotResponse = false;
            int retries = maxRetries;
            int recordAmount = maxRecords;
            consumer.subscribe(Collections.singletonList(topic));
            while (recordAmount > 0 && retries > 0) {
                ConsumerRecords<String, GenericRecord> records = consumer.poll(Duration.ofSeconds(5));
                for (ConsumerRecord<String, GenericRecord> rec : records) {
                    System.out.println("Offset: " + rec.offset());
                    Schema schema = rec.value().getSchema();
                    DatumWriter<Object> writer = new GenericDatumWriter<>(schema);
                    JsonEncoder encoder = EncoderFactory.get().jsonEncoder(schema, System.out, pretty);
                    writer.write(rec.value(), encoder);
                    encoder.flush();
                    System.out.println();
                    recordAmount--;
                }
                boolean responsePresent = !records.isEmpty();
                gotResponse = gotResponse || responsePresent;
                if (responsePresent) {
                    retries = maxRetries;
                } else {
                    retries--;
                }
                Thread.sleep(1000);
            }
            if (!gotResponse) {
                fail("No messages in topic");
            }
        } catch (Exception e) {
            fail("Kafka consumer is available", e);
        }
    }

}

