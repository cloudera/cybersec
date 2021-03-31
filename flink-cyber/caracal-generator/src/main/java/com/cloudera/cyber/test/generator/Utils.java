package com.cloudera.cyber.test.generator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonDecoder;
import org.apache.flink.api.java.utils.ParameterTool;

import java.util.Properties;

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

   public static GenericRecord jsonDecodeToAvroGenericRecord(String json, Schema schema) {
      try {
         JsonDecoder jsonDecoder = DecoderFactory.get().jsonDecoder(schema, json);
         DatumReader<GenericRecord> datumReader = new GenericDatumReader<>(schema);
         return datumReader.read(null, jsonDecoder);
      } catch (IOException e) {
         return null;
      }
   }

   public static byte[] jsonDecodeToAvroByteArray(String json, Schema schema) {
      GenericRecord record = jsonDecodeToAvroGenericRecord(json, schema);
      DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(schema);
      try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
         BinaryEncoder binaryEncoder = EncoderFactory.get().directBinaryEncoder(out, null);
         datumWriter.write(record, binaryEncoder);
         return out.toByteArray();
      } catch (IOException exception) {
         return null;
      }
   }
}
